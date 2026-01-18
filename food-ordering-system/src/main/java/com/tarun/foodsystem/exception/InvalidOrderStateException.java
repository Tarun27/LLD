package com.tarun.foodsystem.exception;

/**
 * Exception thrown when an order operation is invalid for the current order state.
 */
public class InvalidOrderStateException extends FoodOrderingException {
    public InvalidOrderStateException(String message) {
        super(message);
    }
}
