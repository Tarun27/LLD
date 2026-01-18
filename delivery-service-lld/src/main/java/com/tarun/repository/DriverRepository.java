package com.tarun.repository;

import com.tarun.model.Driver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DriverRepository {
    private static DriverRepository instance;
    private final Map<String, Driver> drivers = new ConcurrentHashMap<>();

    /**
     * Private constructor for singleton pattern
     */
    private DriverRepository() {}

    /**
     * Returns the singleton instance of DriverRepository
     */
    public static synchronized DriverRepository getInstance() {
        if (instance == null) {
            instance = new DriverRepository();
        }
        return instance;
    }

    public void save(Driver driver) {
        drivers.put(driver.getId(), driver);
    }

    public Driver findById(String id) {
        return drivers.get(id);
    }

    public boolean exists(String id) {
        return drivers.containsKey(id);
    }

    public List<Driver> findAvailableDrivers() {
        return drivers.values().stream()
                .filter(Driver::isAvailable)
                .collect(Collectors.toList());
    }

    public List<Driver> getAllDrivers() {
        return new ArrayList<>(drivers.values());
    }
}
