package com.tarun.model;


import java.time.LocalDateTime;

public class Order {
    private final String id;
    private final String customerId;
    private final String itemId;
    private final LocalDateTime createdAt;
    private volatile OrderStatus status;
    private volatile String assignedDriverId;
    private LocalDateTime pickedUpAt;
    private LocalDateTime deliveredAt;

    public Order(String id, String customerId, String itemId) {
        this.id = id;
        this.customerId = customerId;
        this.itemId = itemId;
        this.createdAt = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }

    public String getId() { return id; }
    public String getCustomerId() { return customerId; }
    public String getItemId() { return itemId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public synchronized OrderStatus getStatus() { return status; }
    public synchronized void setStatus(OrderStatus status) { this.status = status; }

    public synchronized String getAssignedDriverId() { return assignedDriverId; }
    public synchronized void setAssignedDriverId(String driverId) { this.assignedDriverId = driverId; }

    public synchronized LocalDateTime getPickedUpAt() { return pickedUpAt; }
    public synchronized void setPickedUpAt(LocalDateTime pickedUpAt) { this.pickedUpAt = pickedUpAt; }

    public synchronized LocalDateTime getDeliveredAt() { return deliveredAt; }
    public synchronized void setDeliveredAt(LocalDateTime deliveredAt) { this.deliveredAt = deliveredAt; }
}