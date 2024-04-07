# Basket splitter app
### Problem
The problem of splitting items in basket is similar to Set cover problem.
The elements (items) of universe (basket) belongs to subsets (delivery types).
The goal here is to find minimal set of subsets that which sum consists of all elements in universe.


## Algorithm
Due to class of problem (NP-complete), the algorithm chosen in this implementation is greedy with few optimizations.
### Steps
1. Assign items to delivery type to obtain map, where key is delivery type, and value is multiset with items that can be delivery with this delivery type.
2. Find minimal delivery groups.
3. Find group in groups provided from previous step in which exists delivery type with the highest number of items from basket.
4. Create map with only delivery types returned from previous step, where key is delivery type, and value is multiset with items that can be delivery with this delivery type.
5. Assign items from basket to delivery types ensuring to create the biggest possible article group.


### Optimizations
* Power set is sorted from sets with minimal number of elements - this ensures algorithm will firstly check delivery groups with the smallest number of delivery types.
* After finding first solution, algorithm remembers number of delivery types in that solution to check only delivery groups with the same number of delivery types.

## External libraries
* Jackson - reading json files
* Guava - multiset
## Remarks
* Throwing exceptions in constructor is highly undesirable, that's why if there is any problem with config file an empty map is returned. 
Personally I would change construstor's access modifier to private and create factory method, where exceptions can be thrown.
(I believe the class signature provided in files shouldn't be changed.)
