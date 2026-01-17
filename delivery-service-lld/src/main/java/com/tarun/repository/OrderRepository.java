package com.tarun.repository;

import com.tarun.model.Order;
import com.tarun.model.OrderStatus;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class OrderRepository {
    private final Map<String, Order> orders = new ConcurrentHashMap<>();

    public void save(Order order) {
        orders.put(order.getId(), order);
    }

    public Order findById(String id) {
        return orders.get(id);
    }

    public boolean exists(String id) {
        return orders.containsKey(id);
    }

    public List<Order> findPendingOrders() {
        return orders.values().stream()
                .filter(o -> o.getStatus() == OrderStatus.PENDING)
                .collect(Collectors.toList());
    }

    public List<Order> findAssignedOrders() {
        return orders.values().stream()
                .filter(o -> o.getStatus() == OrderStatus.ASSIGNED)
                .collect(Collectors.toList());
    }
}
