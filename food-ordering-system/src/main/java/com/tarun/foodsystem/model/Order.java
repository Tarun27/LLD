package com.tarun.foodsystem.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents a customer order in the food ordering system.
 * Thread-safe with volatile fields. External synchronization via OrderService lock.
 */
public class Order {
    private final String orderId;
    private final String customerName;
    private final List<OrderItem> items;
    private final SelectionCriteria selectionCriteria;
    private final LocalDateTime createdAt;

    private final Object statusLock = new Object();
    private volatile OrderStatus status;
    private volatile String assignedRestaurantId;
    private volatile BigDecimal totalCost;
    private volatile LocalDateTime completedAt;

    public Order(String customerName, List<OrderItem> items, SelectionCriteria selectionCriteria) {
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be null or empty");
        }
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        if (selectionCriteria == null) {
            throw new IllegalArgumentException("Selection criteria cannot be null");
        }

        this.orderId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.customerName = customerName.trim();
        this.items = Collections.unmodifiableList(items);
        this.selectionCriteria = selectionCriteria;
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.totalCost = BigDecimal.ZERO;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public SelectionCriteria getSelectionCriteria() {
        return selectionCriteria;
    }

    public OrderStatus getStatus() {
        return status;
    }

    /**
     * Sets the order status with validation for valid state transitions.
     * Synchronized to ensure atomicity of check-then-act operation.
     * Valid transitions:
     * - PENDING -> ACCEPTED, REJECTED
     * - ACCEPTED -> COMPLETED
     * - COMPLETED, REJECTED -> (terminal states, no transitions allowed)
     */
    public void setStatus(OrderStatus newStatus) {
        synchronized (statusLock) {
            if (newStatus == null) {
                throw new IllegalArgumentException("Status cannot be null");
            }
            if (!isValidTransition(this.status, newStatus)) {
                throw new IllegalStateException(
                    String.format("Invalid state transition from %s to %s for order %s",
                        this.status, newStatus, orderId));
            }
            this.status = newStatus;
        }
    }

    /**
     * Validates if a state transition is allowed.
     */
    private boolean isValidTransition(OrderStatus from, OrderStatus to) {
        if (from == to) {
            return true; // Same state is always valid (idempotent)
        }
        switch (from) {
            case PENDING:
                return to == OrderStatus.ACCEPTED || to == OrderStatus.REJECTED;
            case ACCEPTED:
                return to == OrderStatus.COMPLETED;
            case COMPLETED:
            case REJECTED:
                return false; // Terminal states
            default:
                return false;
        }
    }

    public String getAssignedRestaurantId() {
        return assignedRestaurantId;
    }

    public void setAssignedRestaurantId(String assignedRestaurantId) {
        this.assignedRestaurantId = assignedRestaurantId;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        if (totalCost == null || totalCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total cost cannot be null or negative");
        }
        this.totalCost = totalCost;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(orderId, order.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId);
    }

    @Override
    public String toString() {
        return String.format("Order[id=%s, customer=%s, status=%s, restaurant=%s, cost=Rs.%.2f]",
                orderId, customerName, status, assignedRestaurantId, totalCost);
    }
}
