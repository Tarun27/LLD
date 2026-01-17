package com.tarun.strategy;

import com.tarun.model.Driver;

import java.util.List;
import java.util.Comparator;

public interface DriverRankingStrategy {
    List<Driver> rankDrivers(List<Driver> drivers);
}

