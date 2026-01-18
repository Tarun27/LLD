package com.tarun.foodsystem.exception;

/**
 * Base exception for food ordering system.
 */
public class FoodOrderingException extends RuntimeException {
    public FoodOrderingException(String message) {
        super(message);
    }

    public FoodOrderingException(String message, Throwable cause) {
        super(message, cause);
    }
}
