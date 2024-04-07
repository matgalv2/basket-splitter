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
import com.ocado.basket.utils.DeliveryTypeSetComparator;

public class BasketSplitter {
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

    public Map<String, List<String>> split(List<String> items) {
        Map<String, List<String>> assignedItemsToDeliveryGroups = assignItemsToDeliveryGroups(items);

        System.out.println("\nAssigned items to delivery groups:");
        System.out.println(assignedItemsToDeliveryGroups);

        List<Set<String>> deliveryGroups = findMinimalDeliveryGroups(items, assignedItemsToDeliveryGroups);

        System.out.println("\nDelivery groups with minimal number of delivery types:");
        System.out.println(deliveryGroups);

        Set<String> bestDeliveryGroup = getDeliveryGroupWithTheMostItems(deliveryGroups, assignedItemsToDeliveryGroups);

        System.out.println("\nDelivery group with the largest number of products for one group:");
        System.out.println(bestDeliveryGroup);

        Map<String, List<String>> assignedItemsToBestDeliveryGroup = new HashMap<>(assignedItemsToDeliveryGroups);
        assignedItemsToBestDeliveryGroup.keySet().retainAll(bestDeliveryGroup);

        System.out.println("\nAssigned items to delivery types from best delivery group:");
        System.out.println(assignedItemsToBestDeliveryGroup);

        Map<String, List<String>> finalAssignment = assignItemsFromBasketToSpecificDeliveryTypes(items, assignedItemsToBestDeliveryGroup);
        System.out.println("\nFinal assignment:");
        System.out.println(finalAssignment);


        return finalAssignment;
    }

    private Map<String, List<String>> assignItemsFromBasketToSpecificDeliveryTypes(List<String> items, Map<String, List<String>> assignedItemsToBestDeliveryGroup){
        // #1 step - choosing the largest item group
        String currentDeliveryType = assignedItemsToBestDeliveryGroup.entrySet()
                .stream()
                .max(Comparator.comparingInt(deliveryType -> deliveryType.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse("");

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
                currentListOfProducts = Multisets.difference(HashMultiset.create(assignedItemsToBestDeliveryGroup.get(currentDeliveryType)), itemsAssigned);
            }

        }
        return finalAssignment;
    }

    private Set<String> getDeliveryGroupWithTheMostItems(List<Set<String>> deliveryGroups, Map<String, List<String>> assignedItemsToDeliveryGroups){
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

    public List<Set<String>> findMinimalDeliveryGroups(List<String> items, Map<String, List<String>> assignedItemsToDeliveryGroups){

        Set<String> uniqueItems = new HashSet<>(items);
        // #1 optimization - sorting power set to start from sets with the least number of elements
        List<Set<String>> powerSet = Sets.powerSet(availableDeliveryTypes).stream().sorted(new DeliveryTypeSetComparator()).toList();

        List<Set<String>> groups = new LinkedList<>();
        int minNumberOfDeliveryTypes = 0;
        boolean groupSizeExceededMinimal = false;

        for (ListIterator<Set<String>> iterator = powerSet.listIterator(); iterator.hasNext() && !groupSizeExceededMinimal;) {

            Set<String> currentDeliveryGroup = iterator.next();
            // #2 optimization - after finding first set of delivery types fulfilling requirements checking only sets with the same number of delivery types
            if(!groups.isEmpty() && currentDeliveryGroup.size() > minNumberOfDeliveryTypes)
                groupSizeExceededMinimal = true;

            Set<String> itemsForCurrentDeliveryGroup = new HashSet<>();
            currentDeliveryGroup.forEach(deliveryType -> itemsForCurrentDeliveryGroup.addAll(assignedItemsToDeliveryGroups.get(deliveryType)));

            if(Sets.difference(uniqueItems, itemsForCurrentDeliveryGroup).isEmpty() && (minNumberOfDeliveryTypes == 0 || currentDeliveryGroup.size() == minNumberOfDeliveryTypes)){
                minNumberOfDeliveryTypes = currentDeliveryGroup.size();
                groups.add(currentDeliveryGroup);
            }
        }
        return groups;
    }

    private Map<String, List<String>> assignItemsToDeliveryGroups(List<String> items){
        Map<String, List<String>> assignedItemsToDeliveryGroups = new HashMap<>();

        availableDeliveryTypes.forEach(deliveryType -> assignedItemsToDeliveryGroups.put(deliveryType, new LinkedList<>()));

        for(String item : items){
            for(String deliveryType : deliveryTypesForProducts.get(item)){
                List<String> currentProducts = assignedItemsToDeliveryGroups.get(deliveryType);
                currentProducts.add(item);
                assignedItemsToDeliveryGroups.put(deliveryType, currentProducts);
            }
        }
        return assignedItemsToDeliveryGroups;

    }

    public static Map<String, List<String>> readConfig (String path){
        ObjectMapper mapper = new ObjectMapper();
        TypeFactory typeFactory = mapper.getTypeFactory();
        Map<String, List<String>> config = Map.of();
        JavaType type = typeFactory.constructMapType(Map.class,  typeFactory.constructType(String.class), typeFactory.constructCollectionType(List.class, String.class));

        try{
            config = mapper.readValue(new File(path), type);
        }
        catch (Exception exception){
            System.out.println(exception.getMessage());
        }

        return config;
    }

}