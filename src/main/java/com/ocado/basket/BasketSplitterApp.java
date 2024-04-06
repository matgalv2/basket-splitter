package com.ocado.basket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.Sets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BasketSplitterApp {
    public static List<String> readBasket (String path) {
        ObjectMapper mapper = new ObjectMapper();
        TypeFactory typeFactory = mapper.getTypeFactory();
        List<String> basket = List.of();
        try{
            basket = mapper.readValue(new File(path), typeFactory.constructCollectionType(List.class, String.class));
        }
        catch (MismatchedInputException mismatchedInputException){
            System.out.println(Arrays.toString(mismatchedInputException.getStackTrace()));
        }
        catch (FileNotFoundException fileNotFoundException){
            System.out.printf("Could not locate JSON file at path: %s%n", path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return basket;
    }
    public static void main(String[] args){


        List<String> items = readBasket("src/main/resources/basket-1.json");

        BasketSplitter basketSplitter = new BasketSplitter("src/main/resources/config.json");
        System.out.println(items);


        Set<Integer> set1 = new HashSet<>(List.of(1, 2, 3));
        Set<Integer> set2 = new HashSet<>(List.of(1, 2, 3, 4));

        System.out.println(Sets.difference(set2, set1));
    }
}
