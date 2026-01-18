package com.tarun.foodsystem.exception;

/**
 * Exception thrown when an order is not found.
 */
public class OrderNotFoundException extends FoodOrderingException {
    public OrderNotFoundException(String orderId) {
        super("Order not found with ID: " + orderId);
    }
}
