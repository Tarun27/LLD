package com.tarun.foodsystem;

import com.tarun.foodsystem.model.*;
import com.tarun.foodsystem.service.OrderService;
import com.tarun.foodsystem.service.RestaurantService;
import com.tarun.foodsystem.store.InMemoryDataStore;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Facade class providing a simplified interface for the food ordering system.
 * This is the main entry point for interacting with the system.
 */
public class FoodOrderingSystem {
    private final RestaurantService restaurantService;
    private final OrderService orderService;

    public FoodOrderingSystem() {
        this.restaurantService = new RestaurantService();
        this.orderService = new OrderService();
    }

    // ========== Restaurant Operations ==========

    /**
     * Onboards a new restaurant to the system.
     */
    public Restaurant onboardRestaurant(String id, String name, int maxCapacity,
                                        double rating, Map<String, BigDecimal> menuItems) {
        return restaurantService.onboardRestaurant(id, name, maxCapacity, rating, menuItems);
    }

    /**
     * Adds a new menu item to a restaurant.
     */
    public void addMenuItem(String restaurantId, String itemName, BigDecimal price) {
        restaurantService.addMenuItem(restaurantId, itemName, price);
    }

    /**
     * Updates an existing menu item's price.
     */
    public void updateMenuItem(String restaurantId, String itemName, BigDecimal newPrice) {
        restaurantService.updateMenuItem(restaurantId, itemName, newPrice);
    }

    /**
     * Updates restaurant capacity (Bonus requirement).
     */
    public void updateRestaurantCapacity(String restaurantId, int newCapacity) {
        restaurantService.updateCapacity(restaurantId, newCapacity);
    }

    /**
     * Gets a restaurant by ID.
     */
    public Restaurant getRestaurant(String restaurantId) {
        return restaurantService.getRestaurant(restaurantId);
    }

    /**
     * Gets all restaurants.
     */
    public List<Restaurant> getAllRestaurants() {
        return restaurantService.getAllRestaurants();
    }

    /**
     * Displays restaurant status.
     */
    public void displayRestaurantStatus(String restaurantId) {
        restaurantService.displayRestaurantStatus(restaurantId);
    }

    // ========== Order Operations ==========

    /**
     * Places a new order with specified items and selection criteria.
     */
    public Order placeOrder(String customerName, List<OrderItem> items, SelectionCriteria criteria) {
        return orderService.placeOrder(customerName, items, criteria);
    }

    /**
     * Marks an order as completed.
     */
    public void markOrderCompleted(String restaurantId, String orderId) {
        orderService.markOrderCompleted(restaurantId, orderId);
    }

    /**
     * Gets an order by ID.
     */
    public Order getOrder(String orderId) {
        return orderService.getOrder(orderId);
    }

    /**
     * Gets all orders.
     */
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    /**
     * Gets orders for a specific restaurant.
     */
    public List<Order> getOrdersForRestaurant(String restaurantId) {
        return orderService.getOrdersForRestaurant(restaurantId);
    }

    /**
     * Displays order status.
     */
    public void displayOrderStatus(String orderId) {
        orderService.displayOrderStatus(orderId);
    }

    // ========== System Operations ==========

    /**
     * Clears all data from the system (useful for testing).
     */
    public void clearAllData() {
        InMemoryDataStore.resetInstance();
    }

    /**
     * Displays system statistics.
     */
    public void displaySystemStats() {
        System.out.println("\n========== SYSTEM STATISTICS ==========");
        System.out.println("Total Restaurants: " + getAllRestaurants().size());
        System.out.println("Total Orders: " + getAllOrders().size());
        
        long acceptedOrders = getAllOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.ACCEPTED)
                .count();
        long completedOrders = getAllOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.COMPLETED)
                .count();
        long rejectedOrders = getAllOrders().stream()
                .filter(o -> o.getStatus() == OrderStatus.REJECTED)
                .count();
        
        System.out.println("Orders by Status:");
        System.out.println("  - Accepted: " + acceptedOrders);
        System.out.println("  - Completed: " + completedOrders);
        System.out.println("  - Rejected: " + rejectedOrders);
        
        System.out.println("\nRestaurant Capacities:");
        getAllRestaurants().forEach(r -> 
            System.out.println(String.format("  - %s: %d/%d (Available: %d)", 
                r.getName(), r.getCurrentOrders(), r.getMaxCapacity(), r.getRemainingCapacity())));
        System.out.println("========================================\n");
    }
}
