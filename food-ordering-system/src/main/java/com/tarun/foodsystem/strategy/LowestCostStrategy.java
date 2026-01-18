package com.tarun.foodsystem.strategy;

import com.tarun.foodsystem.model.Order;
import com.tarun.foodsystem.model.Restaurant;

import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Strategy that selects the restaurant with the lowest total cost for the order.
 * Tie-breaker: restaurant ID (alphabetical) for deterministic selection.
 */
public class LowestCostStrategy implements RestaurantSelectionStrategy {

    @Override
    public Optional<Restaurant> selectRestaurant(List<Restaurant> eligibleRestaurants, Order order) {
        if (eligibleRestaurants == null || eligibleRestaurants.isEmpty()) {
            return Optional.empty();
        }

        // Calculate cost once per restaurant and store as pairs
        return eligibleRestaurants.stream()
                .map(r -> new AbstractMap.SimpleEntry<>(r, r.calculateOrderCost(order.getItems())))
                .filter(entry -> entry.getValue().isPresent())
                .min(Comparator
                        .comparing((Map.Entry<Restaurant, Optional<BigDecimal>> entry) ->
                                entry.getValue().orElse(BigDecimal.valueOf(Double.MAX_VALUE)))
                        .thenComparing(entry -> entry.getKey().getId())) // Tie-breaker: restaurant ID
                .map(Map.Entry::getKey);
    }

    @Override
    public String getStrategyName() {
        return "Lowest Cost";
    }
}
