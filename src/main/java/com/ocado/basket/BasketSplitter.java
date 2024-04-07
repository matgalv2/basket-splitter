package com.ocado.basket;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.google.common.collect.Sets;
import com.ocado.basket.error.InvalidConfigurationException;

final public class BasketSplitter {
    private final Map<String, List<String>> deliveryTypesForProducts;
    private final Set<String> availableDeliveryTypes;

    public BasketSplitter(String absolutePathToConfigFile) {
        deliveryTypesForProducts = readConfig(absolutePathToConfigFile);

        availableDeliveryTypes =
                deliveryTypesForProducts.values()
                        .stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toSet());
    }

    /**
     * Splits items into possibly the lowest delivery group, which contains delivery
     * type covering the largest number of products.
     * @param items a list of items in basket.
     * @return a map with assigned items to delivery types.
     * @throws InvalidConfigurationException if there were any problems with configuration
     * file for example: invalid json format or config file was not found.
     */

    public Map<String, List<String>> split(List<String> items) throws InvalidConfigurationException {

        if(deliveryTypesForProducts.isEmpty())
            throw new InvalidConfigurationException("Invalid configuration - configuration found: " + deliveryTypesForProducts);

        Map<String, Multiset<String>> assignedItemsToDeliveryGroups = assignItemsToDeliveryGroups(items);

        System.out.println("\nAssigned items to delivery groups:");
        System.out.println(assignedItemsToDeliveryGroups);

        List<Set<String>> deliveryGroups = findMinimalDeliveryGroups(items, assignedItemsToDeliveryGroups);

        System.out.println("\nDelivery groups with minimal number of delivery types:");
        System.out.println(deliveryGroups);

        Set<String> bestDeliveryGroup = getDeliveryGroupWithTheMostItems(deliveryGroups, assignedItemsToDeliveryGroups);

        System.out.println("\nDelivery group with the largest number of products for one group:");
        System.out.println(bestDeliveryGroup);

        Map<String, Multiset<String>> assignedItemsToBestDeliveryGroup = new HashMap<>(assignedItemsToDeliveryGroups);
        assignedItemsToBestDeliveryGroup.keySet().retainAll(bestDeliveryGroup);

        System.out.println("\nAssigned items to delivery types from best delivery group:");
        System.out.println(assignedItemsToBestDeliveryGroup);

        Map<String, List<String>> finalAssignment = assignItemsFromBasketToSpecificDeliveryTypes(items, assignedItemsToBestDeliveryGroup);
        System.out.println("\nFinal assignment:");
        System.out.println(finalAssignment);


        return finalAssignment;
    }

    /**
     * Create map consisting of delivery type as keys and list of items that can be delivered
     * with that delivery type as values.
     * Steps:
     *  1) Creates map with delivery types and empty lists as values.
     *  2) Iterates through items.
     *  2.1) Iterates through delivery types. For each product adds product to its delivery types.
     *  In simpler terms it is reversed config map.
     * @param items a list of items in basket.
     * @return map consisting of delivery type as keys and list of items.
     */
    private Map<String, Multiset<String>> assignItemsToDeliveryGroups(List<String> items){
        Map<String, Multiset<String>> assignedItemsToDeliveryGroups = new HashMap<>();

        availableDeliveryTypes.forEach(deliveryType -> assignedItemsToDeliveryGroups.put(deliveryType, HashMultiset.create()));

        for(String item : items){
            for(String deliveryType : deliveryTypesForProducts.get(item)){
                Multiset<String> currentProducts = assignedItemsToDeliveryGroups.get(deliveryType);
                currentProducts.add(item);
                assignedItemsToDeliveryGroups.put(deliveryType, currentProducts);
            }
        }
        return assignedItemsToDeliveryGroups;
    }

    /**
     * Find delivery groups with the smallest amount of delivery types.
     * Steps:
     *  1) Creates a set from item list.
     *  2) Creates power set from delivery types set and sorts it in ascending order, where key is number of delivery types.
     *  3) Creates variables necessary fo loop.
     *  4) Iterates through power set using iterator.
     *  4.1) For each group checks if group cover all items in basket.
     *  In simpler terms it is reversed config map.
     * @param items a list of items in basket.
     * @param assignedItemsToDeliveryGroups a map containing delivery types as keys and multisets with items as values.
     * @return a list containing groups with minimal number of delivery types that covers all items in basket.
     */
    private List<Set<String>> findMinimalDeliveryGroups(List<String> items, Map<String, Multiset<String>> assignedItemsToDeliveryGroups){

        Set<String> uniqueItems = new HashSet<>(items);
        // #1 optimization - sorting power set to start from sets with the least number of elements
        List<Set<String>> powerSet = Sets.powerSet(availableDeliveryTypes).stream().sorted(Comparator.comparingInt(Set::size)).toList();

        List<Set<String>> groups = new LinkedList<>();
        int minNumberOfDeliveryTypes = 0;
        boolean groupSizeExceededMinimal = false;

        for (ListIterator<Set<String>> iterator = powerSet.listIterator(); iterator.hasNext() && !groupSizeExceededMinimal;) {

            Set<String> currentDeliveryGroup = iterator.next();
            // #2 optimization - after finding first set of delivery types fulfilling requirements checking only sets with the same number of delivery types
            if(!groups.isEmpty() && currentDeliveryGroup.size() > minNumberOfDeliveryTypes){
                groupSizeExceededMinimal = true;
            }
            else{
                Set<String> itemsForCurrentDeliveryGroup = new HashSet<>();
                currentDeliveryGroup.forEach(deliveryType -> itemsForCurrentDeliveryGroup.addAll(assignedItemsToDeliveryGroups.get(deliveryType)));

                if(Sets.difference(uniqueItems, itemsForCurrentDeliveryGroup).isEmpty() && (minNumberOfDeliveryTypes == 0 || currentDeliveryGroup.size() == minNumberOfDeliveryTypes)){
                    minNumberOfDeliveryTypes = currentDeliveryGroup.size();
                    groups.add(currentDeliveryGroup);
                }
            }
        }
        return groups;
    }

    /**
     * Searches for the delivery group, which consists of delivery type covering the largest number of products.
     * Steps:
     *  1) Creates necessary variables.
     *  2) Iterates through delivery groups.
     *  2.1) Iterates through delivery types in each group. For each delivery type checks if it is covering the largest number of products.
     * @param deliveryGroups a list of sets containing delivery groups.
     * @param assignedItemsToDeliveryGroups a map containing delivery types as keys and multisets with items as values.
     * @return a set with delivery types, which consists of delivery type covering the largest number of products.
     */
    private Set<String> getDeliveryGroupWithTheMostItems(List<Set<String>> deliveryGroups, Map<String, Multiset<String>> assignedItemsToDeliveryGroups){
        Set<String> bestDeliveryGroup = new HashSet<>();
        int mostProductsForOneDeliveryType = 0;

        for (Set<String> deliveryGroup : deliveryGroups) {
            for(String deliveryType : deliveryGroup){
                int productsForCurrentDeliveryType = assignedItemsToDeliveryGroups.get(deliveryType).size();
                if(productsForCurrentDeliveryType > mostProductsForOneDeliveryType){
                    bestDeliveryGroup = deliveryGroup;
                    mostProductsForOneDeliveryType = productsForCurrentDeliveryType;
                }
            }
        }
        return bestDeliveryGroup;
    }

    /**
     * Searches for the delivery group, which consists of delivery type covering the largest number of products.
     * Steps:
     *  1) Creates necessary variable.
     *  2) Creates variable containing delivery type covering the largest number of products.
     *  3) Checks if found variable is not empty (variable is empty only if item list is empty).
     *  4) Creates necessary variables for loop.
     *  5) Iterates through delivery types in *the best group*. For each delivery type adds its
     *  items to delivery type - starts from variable containing delivery type covering the
     *  largest number of products. Additionally checks if all delivery types have been used.
     * @param items a list of items in basket.
     * @param assignedItemsToBestDeliveryGroup a map containing delivery types from *the best group* as keys and multisets with items as values.
     * @return a map with final assignment of items to delivery types in form K - delivery type, V - list of items.
     */

    private Map<String, List<String>> assignItemsFromBasketToSpecificDeliveryTypes(List<String> items, Map<String, Multiset<String>> assignedItemsToBestDeliveryGroup){
        String empty = "EMPTY";
        // #1 step - choosing the largest item group
        String currentDeliveryType = assignedItemsToBestDeliveryGroup.entrySet()
                .stream()
                .max(Comparator.comparingInt(deliveryType -> deliveryType.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse(empty);

        // in case of empty basket
        if(currentDeliveryType.equals(empty))
            return Map.of();
        else{
            // 2# step - assign items to final delivery type
            Set<String> deliveryTypesLeft = new HashSet<>(assignedItemsToBestDeliveryGroup.keySet());
            Multiset<String> currentListOfProducts = HashMultiset.create(assignedItemsToBestDeliveryGroup.get(currentDeliveryType));

            Map<String, List<String>> finalAssignment = new HashMap<>();
            Multiset<String> itemsLeft = HashMultiset.create(items);
            Multiset<String> itemsAssigned = HashMultiset.create();

            for(int i = 0; i < assignedItemsToBestDeliveryGroup.size(); i++) {
                deliveryTypesLeft.remove(currentDeliveryType);
                finalAssignment.put(currentDeliveryType, currentListOfProducts.stream().toList());
                itemsLeft.removeAll(currentListOfProducts);
                itemsAssigned.addAll(currentListOfProducts);

                if(!deliveryTypesLeft.isEmpty()){
                    currentDeliveryType = deliveryTypesLeft.stream().findFirst().get();
                    currentListOfProducts = Multisets.difference(assignedItemsToBestDeliveryGroup.get(currentDeliveryType), itemsAssigned);
                }

            }
            return finalAssignment;
        }
    }


    /**
     *  Reads configuration file and create map with items and delivery types.
     *  In case of any error along the way an empty map is returned.
     *
     * @param path absolute path to configuration file.
     * @return a map consisting of items as keys and list of delivery types
     * as values or an empty map if configuration file could not be read.
     */
    private Map<String, List<String>> readConfig (String path){
        ObjectMapper mapper = new ObjectMapper();
        TypeFactory typeFactory = mapper.getTypeFactory();
        Map<String, List<String>> config = Map.of();
        JavaType type = typeFactory.constructMapType(Map.class,  typeFactory.constructType(String.class), typeFactory.constructCollectionType(List.class, String.class));

        try{
            config = mapper.readValue(new File(path), type);
        }
        catch (Exception exception){
            exception.printStackTrace();
        }
        return config;
    }

}