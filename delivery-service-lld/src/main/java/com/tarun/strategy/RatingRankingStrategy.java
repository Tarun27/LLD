package com.tarun.strategy;

import com.tarun.model.Driver;

import java.util.List;
import java.util.stream.Collectors;
import java.util.Comparator;

public class RatingRankingStrategy implements DriverRankingStrategy {

    @Override
    public List<Driver> rankDrivers(List<Driver> drivers) {
        return drivers.stream()
                .sorted(Comparator.comparingDouble(Driver::getAverageRating).reversed())
                .collect(Collectors.toList());
    }
}
