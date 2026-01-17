package com.tarun.repository;

import com.tarun.model.Customer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class CustomerRepository {
    private final Map<String, Customer> customers = new ConcurrentHashMap<>();

    public void save(Customer customer) {
        customers.put(customer.getId(), customer);
    }

    public Customer findById(String id) {
        return customers.get(id);
    }

    public boolean exists(String id) {
        return customers.containsKey(id);
    }
}