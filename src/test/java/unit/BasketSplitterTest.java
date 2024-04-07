package unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.ocado.basket.BasketSplitterForTests;
import com.ocado.basket.error.InvalidConfigurationException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class BasketSplitterTest{

    private BasketSplitterForTests basketSplitterForTests;

    @Before
    public void setup(){
        basketSplitterForTests = new BasketSplitterForTests("src/test/resources/config.json");
    }


    private static List<String> readBasket (String path) {
        ObjectMapper mapper = new ObjectMapper();
        TypeFactory typeFactory = mapper.getTypeFactory();
        List<String> basket = List.of();
        try{
            basket = mapper.readValue(new File(path), typeFactory.constructCollectionType(List.class, String.class));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return basket;
    }

    @Test
    public void readConfig_fileNotFound_errorNotThrown(){
        String pathToConfigFile = "\\src\test\\resources";
        int error = -1;
        Map<String, List<String>> config = new HashMap<>();
        config.put("empty", List.of());
        try{
            config = BasketSplitterForTests.readConfig(pathToConfigFile);
            error = 0;
        }
        catch(Exception exception){
            error = 1;
        }

        assertEquals(error, 0);
        assertEquals(config, Map.of());
    }

    @Test
    public void readConfig_invalidJsonFormat_errorNotThrown(){
        String pathToConfigFile = "src/test/resources/config-test.json";
        int error = -1;
        Map<String, List<String>> config = new HashMap<>();
        config.put("empty", List.of());
        try{
            config = BasketSplitterForTests.readConfig(pathToConfigFile);
            error = 0;
        }
        catch(Exception exception){
            error = 1;
        }

        assertEquals(error, 0);
        assertEquals(config, Map.of());
    }

    @Test
    public void assignItemsToDeliveryGroups_emptyItemList(){
        List<String> items = List.of();

        Map<String, Multiset<String>> expected = new HashMap<>();
        expected.put("In-store pick-up",HashMultiset.create());
        expected.put("Next day shipping",HashMultiset.create());
        expected.put("Mailbox delivery",HashMultiset.create());
        expected.put("Parcel locker",HashMultiset.create());

        Map<String, Multiset<String>> result = basketSplitterForTests.assignItemsToDeliveryGroups(items);

        assertEquals(expected, result);
    }

    @Test
    public void assignItemsToDeliveryGroups_correctAssignment(){
        List<String> items = List.of("Haggis", "Longan", "Emulsifier", "Corn Syrup", "Cocoa Butter", "Beans");

        Map<String, Multiset<String>> expected = new HashMap<>();
        expected.put("In-store pick-up",HashMultiset.create(List.of("Haggis", "Longan", "Emulsifier")));
        expected.put("Next day shipping",HashMultiset.create(List.of("Haggis", "Corn Syrup")));
        expected.put("Mailbox delivery",HashMultiset.create(List.of("Longan","Cocoa Butter")));
        expected.put("Parcel locker",HashMultiset.create(List.of("Emulsifier","Beans")));


        Map<String, Multiset<String>> result = basketSplitterForTests.assignItemsToDeliveryGroups(items);
        assertEquals(expected, result);
    }


    @Test
    public void findMinimalDeliveryGroup_correct(){
        List<String> items = List.of("Haggis", "Longan", "Emulsifier", "Corn Syrup", "Cocoa Butter", "Beans");
        Map<String, Multiset<String>> assignedItemsToDeliveryGroups = basketSplitterForTests.assignItemsToDeliveryGroups(items);

        List<Set<String>> result = basketSplitterForTests.findMinimalDeliveryGroups(items, assignedItemsToDeliveryGroups);

        Set<String> expectedSet = new HashSet<>(List.of("Next day shipping","Mailbox delivery","Parcel locker"));
        List<Set<String>> expected = List.of(expectedSet);

        assertEquals(expected, result);
    }


    @Test
    public void findMinimalDeliveryGroup_emptyItemList(){
        List<String> items = List.of();
        Map<String, Multiset<String>> assignedItemsToDeliveryGroups = basketSplitterForTests.assignItemsToDeliveryGroups(items);

        List<Set<String>> result = basketSplitterForTests.findMinimalDeliveryGroups(items, assignedItemsToDeliveryGroups);

        List<Set<String>> expected = List.of(new HashSet<>(List.of()));

        assertEquals(expected, result);
    }

    @Test
    public void findMinimalDeliveryGroup_emptyAssignItemsToDeliveryGroupsMap(){
        List<String> items = List.of("Haggis", "Longan", "Emulsifier", "Corn Syrup", "Cocoa Butter", "Beans");
        Map<String, Multiset<String>> assignedItemsToDeliveryGroups = Map.of();

        List<Set<String>> result = basketSplitterForTests.findMinimalDeliveryGroups(items, assignedItemsToDeliveryGroups);

        List<Set<String>> expected = List.of();

        assertEquals(expected, result);
    }

    @Test
    public void getDeliveryGroupWithTheMostItems_correct(){
        List<Set<String>> deliveryGroups = List.of(Set.of("In-store pick-up", "Mailbox delivery","Parcel locker"), Set.of("Next day shipping","Mailbox delivery","Parcel locker"));

        BasketSplitterForTests basketSplitterForTests2 = new BasketSplitterForTests("src/test/resources/config2.json");

        List<String> items = List.of("Haggis", "Longan", "Emulsifier", "Corn Syrup", "Cocoa Butter", "Beans");

        Map<String, Multiset<String>> assignedItemsToDeliveryGroups = basketSplitterForTests2.assignItemsToDeliveryGroups(items);

        Set<String> result = basketSplitterForTests.getDeliveryGroupWithTheMostItems(deliveryGroups, assignedItemsToDeliveryGroups);

        Set<String> expected = Set.of("In-store pick-up", "Mailbox delivery","Parcel locker");

        assertEquals(expected, result);
    }

    @Test
    public void getDeliveryGroupWithTheMostItems_emptyDeliveryGroups(){
        List<Set<String>> deliveryGroups = List.of();

        BasketSplitterForTests basketSplitterForTests2 = new BasketSplitterForTests("src/test/resources/config2.json");

        List<String> items = List.of("Haggis", "Longan", "Emulsifier", "Corn Syrup", "Cocoa Butter", "Beans");

        Map<String, Multiset<String>> assignedItemsToDeliveryGroups = basketSplitterForTests2.assignItemsToDeliveryGroups(items);

        Set<String> result = basketSplitterForTests.getDeliveryGroupWithTheMostItems(deliveryGroups, assignedItemsToDeliveryGroups);

        Set<String> expected = Set.of();

        assertEquals(expected, result);
    }


    @Test
    public void getDeliveryGroupWithTheMostItems_emptyAssignItemsToDeliveryGroupsMap(){
        List<Set<String>> deliveryGroups = List.of(Set.of("In-store pick-up", "Mailbox delivery","Parcel locker"), Set.of("Next day shipping","Mailbox delivery","Parcel locker"));

        Map<String, Multiset<String>> assignedItemsToDeliveryGroups = Map.of();

        Set<String> result = basketSplitterForTests.getDeliveryGroupWithTheMostItems(deliveryGroups, assignedItemsToDeliveryGroups);

        Set<String> expected = Set.of();

        assertEquals(expected, result);
    }


    @Test
    public void assignItemsFromBasketToSpecificDeliveryTypes_correct(){
        List<String> items = List.of("Haggis", "Longan", "Emulsifier", "Corn Syrup", "Cocoa Butter", "Beans");

        BasketSplitterForTests basketSplitterForTests2 = new BasketSplitterForTests("src/test/resources/config2.json");

        Map<String, Multiset<String>> assignedItemsToDeliveryGroups = basketSplitterForTests2.assignItemsToDeliveryGroups(items);

        Set<String> bestGroup = Set.of("In-store pick-up", "Mailbox delivery","Parcel locker");

        Map<String, Multiset<String>> assignedItemsToBestDeliveryGroup = new HashMap<>(assignedItemsToDeliveryGroups);
        assignedItemsToBestDeliveryGroup.keySet().retainAll(bestGroup);

        Map<String, List<String>> result = basketSplitterForTests2.assignItemsFromBasketToSpecificDeliveryTypes(items, assignedItemsToBestDeliveryGroup);

        Map<String, List<String>> expected = Map.of("In-store pick-up", List.of("Longan", "Corn Syrup", "Emulsifier", "Haggis"), "Mailbox delivery", List.of("Cocoa Butter"), "Parcel locker", List.of("Beans"));

        assertEquals(expected, result);
    }

    @Test
    public void assignItemsFromBasketToSpecificDeliveryTypes_emptyItemList(){
        List<String> items = List.of();

        BasketSplitterForTests basketSplitterForTests2 = new BasketSplitterForTests("src/test/resources/config2.json");

        Map<String, Multiset<String>> assignedItemsToDeliveryGroups = basketSplitterForTests2.assignItemsToDeliveryGroups(items);

        Set<String> bestGroup = Set.of();

        Map<String, Multiset<String>> assignedItemsToBestDeliveryGroup = new HashMap<>(assignedItemsToDeliveryGroups);
        assignedItemsToBestDeliveryGroup.keySet().retainAll(bestGroup);

        Map<String, List<String>> result = basketSplitterForTests2.assignItemsFromBasketToSpecificDeliveryTypes(items, assignedItemsToBestDeliveryGroup);

        Map<String, List<String>> expected = Map.of();

        assertEquals(expected, result);
    }

    @Test
    public void assignItemsFromBasketToSpecificDeliveryTypes_emptyAssignedItemsToBestDeliveryGroup(){
        List<String> items = List.of("Haggis", "Longan", "Emulsifier", "Corn Syrup", "Cocoa Butter", "Beans");

        BasketSplitterForTests basketSplitterForTests2 = new BasketSplitterForTests("src/test/resources/config2.json");

        Map<String, Multiset<String>> assignedItemsToBestDeliveryGroup = new HashMap<>();

        Map<String, List<String>> result = basketSplitterForTests2.assignItemsFromBasketToSpecificDeliveryTypes(items, assignedItemsToBestDeliveryGroup);

        Map<String, List<String>> expected = Map.of();

        assertEquals(expected, result);
    }

    @Test
    public void split_correct() throws InvalidConfigurationException {
        List<String> items = List.of("Haggis", "Longan", "Emulsifier", "Corn Syrup", "Cocoa Butter", "Beans");

        BasketSplitterForTests basketSplitterForTests2 = new BasketSplitterForTests("src/test/resources/config2.json");


        Map<String, List<String>> result = basketSplitterForTests2.split(items);

        Map<String, List<String>> expected = Map.of("In-store pick-up", List.of("Longan", "Corn Syrup", "Emulsifier", "Haggis"), "Mailbox delivery", List.of("Cocoa Butter"), "Parcel locker", List.of("Beans"));

        assertEquals(expected, result);

    }

    @Test
    public void split_emptyItemList() throws InvalidConfigurationException {
        List<String> items = List.of();

        BasketSplitterForTests basketSplitterForTests2 = new BasketSplitterForTests("src/test/resources/config2.json");


        Map<String, List<String>> result = basketSplitterForTests2.split(items);

        Map<String, List<String>> expected = Map.of();

        assertEquals(expected, result);

    }

    @Test
    public void split_invalidConfigFile(){
        List<String> items = List.of("Haggis", "Longan", "Emulsifier", "Corn Syrup", "Cocoa Butter", "Beans");

        BasketSplitterForTests basketSplitterForTests2 = new BasketSplitterForTests("src/test/resources/config-test.json");

        int error = 0;
        try{
            basketSplitterForTests2.split(items);
        }
        catch (InvalidConfigurationException exception){
            error ++;
        }

        assertEquals(error, 1);

    }


}
