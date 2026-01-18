package com.tarun.strategy;


import com.tarun.model.Driver;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

public class OrderCountRankingStrategy implements DriverRankingStrategy {

    /**
     * Ranks drivers by number of completed orders in descending order
     */
    @Override
    public List<Driver> rankDrivers(List<Driver> drivers) {
        return drivers.stream()
                .sorted(Comparator.comparingInt(Driver::getCompletedOrders).reversed())
                .collect(Collectors.toList());
    }
}
