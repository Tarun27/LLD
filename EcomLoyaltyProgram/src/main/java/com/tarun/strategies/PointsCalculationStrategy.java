package com.tarun.strategies;

import com.tarun.model.UserLevel;

public interface PointsCalculationStrategy {
    double calculatePoints(double amount, UserLevel level);
}