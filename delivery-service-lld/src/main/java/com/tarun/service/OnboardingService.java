package com.tarun.service;


import com.tarun.model.Customer;
import com.tarun.model.Driver;
import com.tarun.repository.CustomerRepository;
import com.tarun.repository.DriverRepository;

public class OnboardingService {

    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;

    /**
     * Creates OnboardingService with singleton repositories
     */
    public OnboardingService() {
        this.customerRepository = CustomerRepository.getInstance();
        this.driverRepository = DriverRepository.getInstance();
    }

    /**
     * Onboards a new customer to the system
     * Returns false if customer ID already exists
     */
    public boolean onboardCustomer(String id, String name) {
        try {
            if (customerRepository.exists(id)) {
                return false;
            }
            Customer customer = new Customer(id, name);
            customerRepository.save(customer);
            System.out.println("Customer onboarded: " + id + " - " + name);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Onboards a new driver to the system
     * Returns false if driver ID already exists
     */
    public boolean onboardDriver(String id, String name) {
        try {
            if (driverRepository.exists(id)) {
                return false;
            }
            Driver driver = new Driver(id, name);
            driverRepository.save(driver);
            System.out.println("Driver onboarded: " + id + " - " + name);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Customer getCustomer(String id) {
        return customerRepository.findById(id);
    }

    public Driver getDriver(String id) {
        return driverRepository.findById(id);
    }
}