package com.ocado.basket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.ocado.basket.error.InvalidConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class BasketSplitterApp {
    public static List<String> readBasket (String path) {
        ObjectMapper mapper = new ObjectMapper();
        TypeFactory typeFactory = mapper.getTypeFactory();
        List<String> basket = List.of();
        try{
            basket = mapper.readValue(new File(path), typeFactory.constructCollectionType(List.class, String.class));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return basket;
    }
    public static void main(String[] args) throws InvalidConfigurationException {

        List<String> items = readBasket("src/main/resources/basket-1.json");
        BasketSplitter basketSplitter = new BasketSplitter("src/main/resources/config.json");
        System.out.println("Items:\n");
        System.out.println(items);

        basketSplitter.split(items);
    }
}
