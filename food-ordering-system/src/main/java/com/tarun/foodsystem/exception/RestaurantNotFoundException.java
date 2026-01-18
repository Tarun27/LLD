package com.tarun.foodsystem.exception;

/**
 * Exception thrown when a restaurant is not found.
 */
public class RestaurantNotFoundException extends FoodOrderingException {
    public RestaurantNotFoundException(String restaurantId) {
        super("Restaurant not found with ID: " + restaurantId);
    }
}
