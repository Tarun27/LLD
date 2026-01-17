package com.tarun.strategies.impl;

import com.tarun.model.UserLevel;
import com.tarun.strategies.PointsCalculationStrategy;

public class DefaultPointsCalculationStrategy implements PointsCalculationStrategy {

    @Override
    public double calculatePoints(double amount, UserLevel level) {
        return (amount / 100.0) * level.getPointsPerHundred();
    }
}
