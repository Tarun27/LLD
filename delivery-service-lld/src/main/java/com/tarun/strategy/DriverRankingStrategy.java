package com.tarun.strategy;

import com.tarun.model.Driver;

import java.util.List;

/**
 * Strategy interface for ranking drivers based on different criteria
 */
public interface DriverRankingStrategy {

    /**
     * Ranks a list of drivers based on the specific strategy implementation
     * Returns a sorted list with top-ranked drivers first
     */
    List<Driver> rankDrivers(List<Driver> drivers);
}

