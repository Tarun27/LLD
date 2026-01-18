package com.tarun.foodsystem.strategy;

import com.tarun.foodsystem.model.Order;
import com.tarun.foodsystem.model.Restaurant;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Strategy that selects the restaurant with the highest rating.
 * Tie-breaker: restaurant ID (alphabetical) for deterministic selection.
 */
public class HighestRatingStrategy implements RestaurantSelectionStrategy {

    @Override
    public Optional<Restaurant> selectRestaurant(List<Restaurant> eligibleRestaurants, Order order) {
        if (eligibleRestaurants == null || eligibleRestaurants.isEmpty()) {
            return Optional.empty();
        }

        return eligibleRestaurants.stream()
                .max(Comparator
                        .comparingDouble(Restaurant::getRating)
                        .thenComparing(Comparator.comparing(Restaurant::getId).reversed())); // Tie-breaker: restaurant ID (reversed for max)
    }

    @Override
    public String getStrategyName() {
        return "Highest Rating";
    }
}
