package com.tarun.foodsystem.strategy;

import com.tarun.foodsystem.model.SelectionCriteria;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory for creating restaurant selection strategies.
 * Uses Factory pattern combined with registry for extensibility.
 */
public class StrategyFactory {
    private static final Map<SelectionCriteria, RestaurantSelectionStrategy> strategies;

    static {
        strategies = new EnumMap<>(SelectionCriteria.class);
        strategies.put(SelectionCriteria.LOWEST_COST, new LowestCostStrategy());
        strategies.put(SelectionCriteria.HIGHEST_RATING, new HighestRatingStrategy());
        strategies.put(SelectionCriteria.MAX_CAPACITY, new MaxCapacityStrategy());
    }

    /**
     * Gets the strategy for the given selection criteria.
     */
    public static RestaurantSelectionStrategy getStrategy(SelectionCriteria criteria) {
        if (criteria == null) {
            throw new IllegalArgumentException("Selection criteria cannot be null");
        }
        
        RestaurantSelectionStrategy strategy = strategies.get(criteria);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for criteria: " + criteria);
        }
        return strategy;
    }

    /**
     * Registers a custom strategy for a criteria.
     * Allows extensibility for new selection criteria.
     */
    public static void registerStrategy(SelectionCriteria criteria, RestaurantSelectionStrategy strategy) {
        if (criteria == null || strategy == null) {
            throw new IllegalArgumentException("Criteria and strategy cannot be null");
        }
        strategies.put(criteria, strategy);
    }
}
