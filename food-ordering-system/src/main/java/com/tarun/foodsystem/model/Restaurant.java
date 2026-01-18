package com.tarun.foodsystem.model;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents a restaurant in the food ordering system.
 * Thread-safe implementation for concurrent access.
 */
public class Restaurant {
    private final String id;
    private final String name;
    private final double rating;
    private final AtomicInteger maxCapacity;
    private final AtomicInteger currentOrders;
    private final Map<String, MenuItem> menu;

    public Restaurant(String id, String name, int maxCapacity, double rating) {
        validateInputs(id, name, maxCapacity, rating);
        
        this.id = id;
        this.name = name;
        this.maxCapacity = new AtomicInteger(maxCapacity);
        this.rating = rating;
        this.currentOrders = new AtomicInteger(0);
        this.menu = new ConcurrentHashMap<>();
    }

    private void validateInputs(String id, String name, int maxCapacity, double rating) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Restaurant ID cannot be null or empty");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Restaurant name cannot be null or empty");
        }
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Max capacity must be positive");
        }
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 0 and 5");
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMaxCapacity() {
        return maxCapacity.get();
    }

    public double getRating() {
        return rating;
    }

    public int getCurrentOrders() {
        return currentOrders.get();
    }

    /**
     * Gets the remaining capacity (available slots for orders).
     */
    public int getRemainingCapacity() {
        return Math.max(0, maxCapacity.get() - currentOrders.get());
    }

    /**
     * Checks if the restaurant can accept a new order.
     */
    public boolean canAcceptOrder() {
        return currentOrders.get() < maxCapacity.get();
    }

    /**
     * Tries to accept a new order. Returns true if successful.
     * Thread-safe using CAS operation.
     */
    public boolean tryAcceptOrder() {
        while (true) {
            int current = currentOrders.get();
            if (current >= maxCapacity.get()) {
                return false;
            }
            if (currentOrders.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    /**
     * Releases a slot when an order is completed.
     */
    public void releaseOrderSlot() {
        currentOrders.updateAndGet(val -> Math.max(0, val - 1));
    }

    /**
     * Adds or updates a menu item.
     */
    public void addOrUpdateMenuItem(String itemName, BigDecimal price) {
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be null or empty");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Price must be positive");
        }
        menu.put(itemName.toLowerCase(), new MenuItem(itemName, price));
    }

    /**
     * Gets a menu item by name (case-insensitive).
     */
    public Optional<MenuItem> getMenuItem(String itemName) {
        if (itemName == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(menu.get(itemName.toLowerCase()));
    }

    /**
     * Checks if the restaurant has a specific menu item.
     */
    public boolean hasMenuItem(String itemName) {
        return itemName != null && menu.containsKey(itemName.toLowerCase());
    }

    /**
     * Gets a copy of the menu.
     */
    public Map<String, MenuItem> getMenu() {
        return new ConcurrentHashMap<>(menu);
    }

    /**
     * Calculates total cost for the given order items.
     * Returns empty if any item is not available.
     */
    public Optional<BigDecimal> calculateOrderCost(java.util.List<OrderItem> items) {
        BigDecimal total = BigDecimal.ZERO;
        for (OrderItem item : items) {
            Optional<MenuItem> menuItem = getMenuItem(item.getItemName());
            if (menuItem.isEmpty()) {
                return Optional.empty();
            }
            total = total.add(menuItem.get().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return Optional.of(total);
    }

    /**
     * Checks if the restaurant can fulfill all items in an order.
     */
    public boolean canFulfillOrder(java.util.List<OrderItem> items) {
        for (OrderItem item : items) {
            if (!hasMenuItem(item.getItemName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Updates the maximum capacity (Bonus requirement).
     */
    public void updateCapacity(int newCapacity) {
        if (newCapacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        maxCapacity.set(newCapacity);
    }

    @Override
    public String toString() {
        return String.format("Restaurant[id=%s, name=%s, rating=%.1f/5, capacity=%d/%d, menu=%d items]",
                id, name, rating, currentOrders.get(), maxCapacity.get(), menu.size());
    }
}
