package com.tarun.foodsystem.store;

import com.tarun.foodsystem.model.Order;
import com.tarun.foodsystem.model.Restaurant;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory data store for restaurants and orders.
 * Thread-safe implementation using ConcurrentHashMap.
 * Singleton pattern for global access.
 */
public class InMemoryDataStore {
    private static volatile InMemoryDataStore instance;
    
    private final Map<String, Restaurant> restaurants;
    private final Map<String, Order> orders;
    private final Map<String, List<String>> restaurantOrders; // restaurantId -> list of orderIds

    private InMemoryDataStore() {
        this.restaurants = new ConcurrentHashMap<>();
        this.orders = new ConcurrentHashMap<>();
        this.restaurantOrders = new ConcurrentHashMap<>();
    }

    /**
     * Gets the singleton instance (double-checked locking for thread safety).
     */
    public static InMemoryDataStore getInstance() {
        if (instance == null) {
            synchronized (InMemoryDataStore.class) {
                if (instance == null) {
                    instance = new InMemoryDataStore();
                }
            }
        }
        return instance;
    }

    // Restaurant operations
    public void saveRestaurant(Restaurant restaurant) {
        restaurants.put(restaurant.getId(), restaurant);
        restaurantOrders.putIfAbsent(restaurant.getId(), Collections.synchronizedList(new ArrayList<>()));
    }

    public Optional<Restaurant> findRestaurantById(String id) {
        return Optional.ofNullable(restaurants.get(id));
    }

    public List<Restaurant> getAllRestaurants() {
        return new ArrayList<>(restaurants.values());
    }

    public boolean restaurantExists(String id) {
        return restaurants.containsKey(id);
    }

    // Order operations
    public void saveOrder(Order order) {
        orders.put(order.getOrderId(), order);
    }

    public Optional<Order> findOrderById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    public List<Order> getAllOrders() {
        return new ArrayList<>(orders.values());
    }

    // Restaurant-Order relationship
    public void addOrderToRestaurant(String restaurantId, String orderId) {
        restaurantOrders.computeIfAbsent(restaurantId, k -> Collections.synchronizedList(new ArrayList<>()))
                .add(orderId);
    }

    public List<String> getOrdersForRestaurant(String restaurantId) {
        return new ArrayList<>(restaurantOrders.getOrDefault(restaurantId, Collections.emptyList()));
    }

    /**
     * Clears all data (useful for testing).
     */
    public void clearAll() {
        restaurants.clear();
        orders.clear();
        restaurantOrders.clear();
    }

    /**
     * Resets the singleton instance (useful for testing).
     */
    public static void resetInstance() {
        synchronized (InMemoryDataStore.class) {
            if (instance != null) {
                instance.clearAll();
            }
        }
    }
}
