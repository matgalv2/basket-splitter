package com.ocado.basket;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Sets;
import com.ocado.basket.utils.DeliveryTypeSetComparator;

public class BasketSplitter {
    private final Map<String, List<String>> deliveryTypesForProducts;
    private final Set<String> availableDeliveryTypes;

    /* ... */
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
        List<Set<String>> deliveryGroups = findMinimalDeliveryGroups(items, assignedItemsToDeliveryGroups);

        Set<String> itemTypes = new HashSet<>(items);
        for (Set<String> deliveryGroup : deliveryGroups) {

        }

        return null;
    }

    public List<Set<String>> findMinimalDeliveryGroups(List<String> items, Map<String, List<String>> assignedItemsToDeliveryGroups){

        Set<String> uniqueItems = new HashSet<>(items);
        // #1 optimization - sorting power set to start from sets with least elements
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
        System.out.println(groups);
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