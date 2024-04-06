package com.ocado.basket.utils;

import java.util.Comparator;
import java.util.Set;

public class DeliveryTypeSetComparator implements Comparator<Set<String>> {
    @Override
    public int compare(Set<String> o1, Set<String> o2) {
        return Integer.compare(o1.size(), o2.size());
    }
}