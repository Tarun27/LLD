package com.tarun.foodsystem.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a menu item in a restaurant's menu.
 * Immutable class for thread safety.
 */
public class MenuItem {
    private final String name;
    private final BigDecimal price;

    public MenuItem(String name, BigDecimal price) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Menu item name cannot be null or empty");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Menu item price must be positive");
        }
        this.name = name.trim();
        this.price = price;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuItem menuItem = (MenuItem) o;
        return name.equalsIgnoreCase(menuItem.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.toLowerCase());
    }

    @Override
    public String toString() {
        return String.format("%s: Rs.%.2f", name, price);
    }
}
