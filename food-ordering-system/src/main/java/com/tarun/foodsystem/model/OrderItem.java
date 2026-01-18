package com.tarun.foodsystem.model;

import java.util.Objects;

/**
 * Represents an item in an order with its quantity.
 */
public class OrderItem {
    private final String itemName;
    private final int quantity;

    public OrderItem(String itemName, int quantity) {
        if (itemName == null || itemName.trim().isEmpty()) {
            throw new IllegalArgumentException("Item name cannot be null or empty");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        this.itemName = itemName.trim();
        this.quantity = quantity;
    }

    public String getItemName() {
        return itemName;
    }

    public int getQuantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return quantity == orderItem.quantity && 
               itemName.equalsIgnoreCase(orderItem.itemName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemName.toLowerCase(), quantity);
    }

    @Override
    public String toString() {
        return String.format("%d x %s", quantity, itemName);
    }
}
