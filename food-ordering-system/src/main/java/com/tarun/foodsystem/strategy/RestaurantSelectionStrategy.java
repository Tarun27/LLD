package com.tarun.foodsystem.strategy;

import com.tarun.foodsystem.model.Order;
import com.tarun.foodsystem.model.Restaurant;

import java.util.List;
import java.util.Optional;

/**
 * Strategy interface for selecting a restaurant for an order.
 * Follows the Strategy design pattern for extensibility.
 */
public interface RestaurantSelectionStrategy {
    
    /**
     * Selects the best restaurant from the list of eligible restaurants.
     * 
     * @param eligibleRestaurants List of restaurants that can fulfill the order
     * @param order The order to be fulfilled
     * @return Optional containing the selected restaurant, or empty if none selected
     */
    Optional<Restaurant> selectRestaurant(List<Restaurant> eligibleRestaurants, Order order);
    
    /**
     * Returns the name/description of this strategy.
     */
    String getStrategyName();
}
