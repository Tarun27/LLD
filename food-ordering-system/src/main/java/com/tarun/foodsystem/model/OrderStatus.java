package com.tarun.foodsystem.model;

/**
 * Enum representing the possible statuses of an order.
 */
public enum OrderStatus {
    PENDING,      // Order is being processed for restaurant assignment
    ACCEPTED,     // Order has been accepted by a restaurant
    COMPLETED,    // Order has been completed by the restaurant
    REJECTED      // Order could not be fulfilled
}
