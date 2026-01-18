package com.tarun.foodsystem.service;

import com.tarun.foodsystem.exception.RestaurantNotFoundException;
import com.tarun.foodsystem.model.MenuItem;
import com.tarun.foodsystem.model.Restaurant;
import com.tarun.foodsystem.store.InMemoryDataStore;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Service for managing restaurant operations.
 * Thread-safe implementation with lock for menu modifications.
 */
public class RestaurantService {
    private final InMemoryDataStore dataStore;
    private final ReentrantLock menuLock;

    public RestaurantService() {
        this.dataStore = InMemoryDataStore.getInstance();
        this.menuLock = new ReentrantLock();
    }

    /**
     * Onboards a new restaurant.
     * 
     * @param id Unique identifier for the restaurant
     * @param name Restaurant name
     * @param maxCapacity Maximum orders that can be processed simultaneously
     * @param rating Restaurant rating (0-5)
     * @param menuItems Map of item names to prices
     * @return The created restaurant
     */
    public Restaurant onboardRestaurant(String id, String name, int maxCapacity, 
                                        double rating, Map<String, BigDecimal> menuItems) {
        if (dataStore.restaurantExists(id)) {
            throw new IllegalArgumentException("Restaurant with ID " + id + " already exists");
        }

        Restaurant restaurant = new Restaurant(id, name, maxCapacity, rating);
        
        if (menuItems != null) {
            menuItems.forEach(restaurant::addOrUpdateMenuItem);
        }
        
        dataStore.saveRestaurant(restaurant);
        System.out.println("Restaurant onboarded: " + restaurant);
        return restaurant;
    }

    /**
     * Adds a new item to a restaurant's menu.
     */
    public void addMenuItem(String restaurantId, String itemName, BigDecimal price) {
        menuLock.lock();
        try {
            Restaurant restaurant = getRestaurantOrThrow(restaurantId);

            if (restaurant.hasMenuItem(itemName)) {
                throw new IllegalArgumentException("Item " + itemName + " already exists. Use updateMenuItem instead.");
            }

            restaurant.addOrUpdateMenuItem(itemName, price);
            System.out.println(String.format("Added menu item '%s' at Rs.%.2f to restaurant %s",
                    itemName, price, restaurantId));
        } finally {
            menuLock.unlock();
        }
    }

    /**
     * Updates an existing menu item's price.
     */
    public void updateMenuItem(String restaurantId, String itemName, BigDecimal newPrice) {
        menuLock.lock();
        try {
            Restaurant restaurant = getRestaurantOrThrow(restaurantId);

            if (!restaurant.hasMenuItem(itemName)) {
                throw new IllegalArgumentException("Item " + itemName + " does not exist in the menu");
            }

            restaurant.addOrUpdateMenuItem(itemName, newPrice);
            System.out.println(String.format("Updated menu item '%s' to Rs.%.2f in restaurant %s",
                    itemName, newPrice, restaurantId));
        } finally {
            menuLock.unlock();
        }
    }

    /**
     * Updates restaurant capacity (Bonus requirement).
     */
    public void updateCapacity(String restaurantId, int newCapacity) {
        menuLock.lock();
        try {
            Restaurant restaurant = getRestaurantOrThrow(restaurantId);
            restaurant.updateCapacity(newCapacity);
            System.out.println(String.format("Updated capacity for restaurant %s to %d",
                    restaurantId, newCapacity));
        } finally {
            menuLock.unlock();
        }
    }

    /**
     * Gets a restaurant by ID.
     */
    public Restaurant getRestaurant(String restaurantId) {
        return getRestaurantOrThrow(restaurantId);
    }

    /**
     * Gets all restaurants.
     */
    public List<Restaurant> getAllRestaurants() {
        return dataStore.getAllRestaurants();
    }

    /**
     * Gets the menu for a restaurant.
     */
    public Map<String, MenuItem> getMenu(String restaurantId) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);
        return restaurant.getMenu();
    }

    /**
     * Displays restaurant status.
     */
    public void displayRestaurantStatus(String restaurantId) {
        Restaurant restaurant = getRestaurantOrThrow(restaurantId);
        System.out.println("\n=== Restaurant Status ===");
        System.out.println("ID: " + restaurant.getId());
        System.out.println("Name: " + restaurant.getName());
        System.out.println("Rating: " + restaurant.getRating() + "/5");
        System.out.println("Current Orders: " + restaurant.getCurrentOrders() + "/" + restaurant.getMaxCapacity());
        System.out.println("Remaining Capacity: " + restaurant.getRemainingCapacity());
        System.out.println("Menu:");
        restaurant.getMenu().values().forEach(item -> 
            System.out.println("  - " + item));
        System.out.println("========================\n");
    }

    private Restaurant getRestaurantOrThrow(String restaurantId) {
        return dataStore.findRestaurantById(restaurantId)
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));
    }
}
