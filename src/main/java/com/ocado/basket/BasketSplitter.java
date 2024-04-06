package com.ocado.basket;

import java.io.File;
import java.util.*;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

public class BasketSplitter {
    private final Map<String, List<String>> deliveryTypesForProducts;
    private final Set<String> availableDeliveryTypes;

    /* ... */
    public BasketSplitter(String absolutePathToConfigFile) {
        deliveryTypesForProducts = readConfig(absolutePathToConfigFile);

        availableDeliveryTypes = new HashSet<>();
        deliveryTypesForProducts.forEach((product, deliveries) -> availableDeliveryTypes.addAll(deliveries));
    }
    public Map<String, List<String>> split(List<String> items) {
        return null;
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