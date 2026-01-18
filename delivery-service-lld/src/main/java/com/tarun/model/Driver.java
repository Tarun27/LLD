package com.tarun.model;

/**
 * Creates a new driver who is initially available for deliveries
 */
public class Driver {
    private final String id;
    private final String name;
    private volatile boolean available;
    private double totalRating;
    private int ratingCount;
    private int completedOrders;

    public Driver(String id, String name) {
        this.id = id;
        this.name = name;
        this.available = true;
        this.totalRating = 0.0;
        this.ratingCount = 0;
        this.completedOrders = 0;
    }

    public String getId() { return id; }
    public String getName() { return name; }

    public synchronized boolean isAvailable() { return available; }
    public synchronized void setAvailable(boolean available) { this.available = available; }

    public synchronized void addRating(double rating) {
        totalRating += rating;
        ratingCount++;
    }

    public synchronized double getAverageRating() {
        return ratingCount > 0 ? totalRating / ratingCount : 0.0;
    }

    public synchronized void incrementCompletedOrders() {
        completedOrders++;
    }

    public synchronized int getCompletedOrders() { return completedOrders; }
    public synchronized int getRatingCount() { return ratingCount; }
}