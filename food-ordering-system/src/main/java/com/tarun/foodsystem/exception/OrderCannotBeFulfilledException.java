package com.tarun.foodsystem.exception;

/**
 * Exception thrown when an order cannot be fulfilled by any restaurant.
 */
public class OrderCannotBeFulfilledException extends FoodOrderingException {
    public OrderCannotBeFulfilledException(String message) {
        super(message);
    }
}
