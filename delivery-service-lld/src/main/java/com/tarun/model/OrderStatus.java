package com.tarun.model;

/**
 * Enum representing the different states an order can be in
 */
public enum OrderStatus {
    PENDING,      // Order placed, waiting for driver assignment
    ASSIGNED,     // Driver assigned, waiting for pickup
    PICKED_UP,    // Driver has picked up the order
    DELIVERED,    // Order successfully delivered
    CANCELLED     // Order cancelled by customer or system
}