package com.tarun.repository;

import com.tarun.model.Customer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class CustomerRepository {
    private static CustomerRepository instance;
    private final Map<String, Customer> customers = new ConcurrentHashMap<>();

    /**
     * Private constructor for singleton pattern
     */
    private CustomerRepository() {}

    /**
     * Returns the singleton instance of CustomerRepository
     */
    public static synchronized CustomerRepository getInstance() {
        if (instance == null) {
            instance = new CustomerRepository();
        }
        return instance;
    }

    public void save(Customer customer) {
        customers.put(customer.getId(), customer);
    }

    /**
     * Finds a customer by their ID
     * Returns null if customer doesn't exist
     */
    public Customer findById(String id) {
        return customers.get(id);
    }

    /**
     * Checks if a customer with the given ID exists
     */
    public boolean exists(String id) {
        return customers.containsKey(id);
    }
}