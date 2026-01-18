package com.tarun.foodsystem;


import com.tarun.foodsystem.exception.OrderCannotBeFulfilledException;
import com.tarun.foodsystem.model.Order;
import com.tarun.foodsystem.model.OrderItem;
import com.tarun.foodsystem.model.Restaurant;
import com.tarun.foodsystem.model.SelectionCriteria;

import java.math.BigDecimal;
import java.util.*;

/**
 * Demo class to test all functionality of the Food Ordering System.
 * Implements all the sample test cases from the requirements.
 */
public class FoodOrderingDemo {

    public static void main(String[] args) {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║         FOOD ORDERING SYSTEM - DEMONSTRATION                  ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        FoodOrderingSystem system = new FoodOrderingSystem();

        try {
            // Test Case 1: Onboard Restaurants
            testOnboardRestaurants(system);

            // Test Case 2: Update Restaurant Menu
            testUpdateMenu(system);

            // Test Case 3: Place Orders
            testPlaceOrders(system);

            // Test Bonus: Update Capacity
            testUpdateCapacity(system);

            // Test Bonus: Max Capacity Selection Criteria
            testMaxCapacityCriteria(system);

            // Display final system stats
            system.displaySystemStats();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void testOnboardRestaurants(FoodOrderingSystem system) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEST CASE 1: ONBOARD RESTAURANTS");
        System.out.println("=".repeat(60) + "\n");

        // R1: max_orders=5, Menu: Veg Biryani Rs.100, Chicken Biryani Rs.150, Rating: 4.5
        Map<String, BigDecimal> menu1 = new LinkedHashMap<>();
        menu1.put("Veg Biryani", new BigDecimal("100"));
        menu1.put("Chicken Biryani", new BigDecimal("150"));
        system.onboardRestaurant("R1", "Restaurant 1", 5, 4.5, menu1);

        // R2: max_orders=5, Menu: Chicken Biryani Rs.175, Idli Rs.10, Dosa Rs.50, Veg Biryani Rs.80, Rating: 4.0
        Map<String, BigDecimal> menu2 = new LinkedHashMap<>();
        menu2.put("Chicken Biryani", new BigDecimal("175"));
        menu2.put("Idli", new BigDecimal("10"));
        menu2.put("Dosa", new BigDecimal("50"));
        menu2.put("Veg Biryani", new BigDecimal("80"));
        system.onboardRestaurant("R2", "Restaurant 2", 5, 4.0, menu2);

        // R3: max_orders=1, Menu: Gobi Manchurian Rs.150, Idli Rs.15, Chicken Biryani Rs.175, Dosa Rs.30, Rating: 4.9
        Map<String, BigDecimal> menu3 = new LinkedHashMap<>();
        menu3.put("Gobi Manchurian", new BigDecimal("150"));
        menu3.put("Idli", new BigDecimal("15"));
        menu3.put("Chicken Biryani", new BigDecimal("175"));
        menu3.put("Dosa", new BigDecimal("30"));
        system.onboardRestaurant("R3", "Restaurant 3", 1, 4.9, menu3);

        System.out.println("\nAll restaurants onboarded successfully!");
        
        // Display all restaurant statuses
        System.out.println("\n--- Restaurant Details ---");
        system.displayRestaurantStatus("R1");
        system.displayRestaurantStatus("R2");
        system.displayRestaurantStatus("R3");
    }

    private static void testUpdateMenu(FoodOrderingSystem system) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEST CASE 2: UPDATE RESTAURANT MENU");
        System.out.println("=".repeat(60) + "\n");

        // ADD: Restaurant_1, add, Chicken65, Rs.250
        System.out.println("Adding Chicken65 to R1...");
        system.addMenuItem("R1", "Chicken65", new BigDecimal("250"));

        // UPDATE: Restaurant_2, update, Chicken Biryani, Rs.150
        System.out.println("Updating Chicken Biryani price in R2...");
        system.updateMenuItem("R2", "Chicken Biryani", new BigDecimal("150"));

        System.out.println("\nMenu updates completed!");
        
        // Display updated menus
        System.out.println("\n--- Updated Menus ---");
        system.displayRestaurantStatus("R1");
        system.displayRestaurantStatus("R2");
    }

    private static void testPlaceOrders(FoodOrderingSystem system) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("TEST CASE 3: PLACE ORDERS");
        System.out.println("=".repeat(60) + "\n");

        // Order1: user=Ashwin, items=[3*Idli, 1*Dosa], selection=Lowest cost
        // Expected: Order assigned to R3 (Idli Rs.15*3 + Dosa Rs.30 = Rs.75)
        // R2 cost would be: Idli Rs.10*3 + Dosa Rs.50 = Rs.80
        System.out.println("--- ORDER 1 ---");
        System.out.println("User: Ashwin, Items: 3 Idli, 1 Dosa, Selection: Lowest Cost");
        System.out.println("Expected: Order assigned to R3 (cost: 3*15 + 30 = Rs.75)");
        
        List<OrderItem> items1 = Arrays.asList(
            new OrderItem("Idli", 3),
            new OrderItem("Dosa", 1)
        );
        Order order1 = system.placeOrder("Ashwin", items1, SelectionCriteria.LOWEST_COST);
        System.out.println("Result: Order " + order1.getOrderId() + " assigned to " + 
            system.getRestaurant(order1.getAssignedRestaurantId()).getName() + 
            " (Cost: Rs." + order1.getTotalCost() + ")");

        // Order2: user=Harish, items=[3*Idli, 1*Dosa], selection=Lowest cost
        // Expected: Order assigned to R2 (R3 is at full capacity)
        System.out.println("\n--- ORDER 2 ---");
        System.out.println("User: Harish, Items: 3 Idli, 1 Dosa, Selection: Lowest Cost");
        System.out.println("Expected: Order assigned to R2 (R3 is at full capacity)");
        
        List<OrderItem> items2 = Arrays.asList(
            new OrderItem("Idli", 3),
            new OrderItem("Dosa", 1)
        );
        Order order2 = system.placeOrder("Harish", items2, SelectionCriteria.LOWEST_COST);
        System.out.println("Result: Order " + order2.getOrderId() + " assigned to " + 
            system.getRestaurant(order2.getAssignedRestaurantId()).getName() + 
            " (Cost: Rs." + order2.getTotalCost() + ")");

        // Order3: user=Shruthi, items=[3*Veg Biryani], selection=Highest rating
        // Expected: Order assigned to R1 (highest rating among those with Veg Biryani)
        System.out.println("\n--- ORDER 3 ---");
        System.out.println("User: Shruthi, Items: 3 Veg Biryani, Selection: Highest Rating");
        System.out.println("Expected: Order assigned to R1 (rating 4.5 > R2's 4.0)");
        
        List<OrderItem> items3 = Arrays.asList(
            new OrderItem("Veg Biryani", 3)
        );
        Order order3 = system.placeOrder("Shruthi", items3, SelectionCriteria.HIGHEST_RATING);
        System.out.println("Result: Order " + order3.getOrderId() + " assigned to " + 
            system.getRestaurant(order3.getAssignedRestaurantId()).getName() + 
            " (Rating: " + system.getRestaurant(order3.getAssignedRestaurantId()).getRating() + "/5)");

        // Mark Order1 as COMPLETED
        System.out.println("\n--- MARK ORDER 1 AS COMPLETED ---");
        System.out.println("R3 marks Order1 as COMPLETED");
        system.markOrderCompleted("R3", order1.getOrderId());

        // Order4: user=Harish, items=[3*Idli, 1*Dosa], selection=Lowest cost
        // Expected: Order assigned to R3 (since R3 has COMPLETED Order1)
        System.out.println("\n--- ORDER 4 ---");
        System.out.println("User: Harish, Items: 3 Idli, 1 Dosa, Selection: Lowest Cost");
        System.out.println("Expected: Order assigned to R3 (capacity freed after completing Order1)");
        
        List<OrderItem> items4 = Arrays.asList(
            new OrderItem("Idli", 3),
            new OrderItem("Dosa", 1)
        );
        Order order4 = system.placeOrder("Harish", items4, SelectionCriteria.LOWEST_COST);
        System.out.println("Result: Order " + order4.getOrderId() + " assigned to " + 
            system.getRestaurant(order4.getAssignedRestaurantId()).getName() + 
            " (Cost: Rs." + order4.getTotalCost() + ")");

        // Order5: user=xyz, items=[1*Paneer Tikka, 1*Idli], selection=Lowest cost
        // Expected: Order can't be fulfilled (no restaurant serves Paneer Tikka)
        System.out.println("\n--- ORDER 5 ---");
        System.out.println("User: xyz, Items: 1 Paneer Tikka, 1 Idli, Selection: Lowest Cost");
        System.out.println("Expected: Order can't be fulfilled (no restaurant serves Paneer Tikka)");
        
        try {
            List<OrderItem> items5 = Arrays.asList(
                new OrderItem("Paneer Tikka", 1),
                new OrderItem("Idli", 1)
            );
            system.placeOrder("xyz", items5, SelectionCriteria.LOWEST_COST);
        } catch (OrderCannotBeFulfilledException e) {
            System.out.println("Result: " + e.getMessage());
        }
    }

    private static void testUpdateCapacity(FoodOrderingSystem system) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("BONUS TEST: UPDATE RESTAURANT CAPACITY");
        System.out.println("=".repeat(60) + "\n");

        System.out.println("Current R3 capacity: " + system.getRestaurant("R3").getMaxCapacity());
        System.out.println("Updating R3 capacity to 3...");
        system.updateRestaurantCapacity("R3", 3);
        System.out.println("New R3 capacity: " + system.getRestaurant("R3").getMaxCapacity());
        System.out.println("R3 remaining capacity: " + system.getRestaurant("R3").getRemainingCapacity());
    }

    private static void testMaxCapacityCriteria(FoodOrderingSystem system) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("BONUS TEST: MAX CAPACITY SELECTION CRITERIA");
        System.out.println("=".repeat(60) + "\n");

        // Display current capacities
        System.out.println("Current restaurant capacities:");
        for (Restaurant r : system.getAllRestaurants()) {
            System.out.println(String.format("  %s: %d/%d (Remaining: %d)", 
                r.getName(), r.getCurrentOrders(), r.getMaxCapacity(), r.getRemainingCapacity()));
        }

        // Place order with MAX_CAPACITY criteria
        System.out.println("\nPlacing order with MAX_CAPACITY selection criteria...");
        System.out.println("User: TestUser, Items: 1 Idli, Selection: Max Capacity");
        
        List<OrderItem> items = Arrays.asList(
            new OrderItem("Idli", 1)
        );
        Order order = system.placeOrder("TestUser", items, SelectionCriteria.MAX_CAPACITY);
        System.out.println("Result: Order " + order.getOrderId() + " assigned to " + 
            system.getRestaurant(order.getAssignedRestaurantId()).getName() + 
            " (Remaining capacity was highest)");
    }
}
