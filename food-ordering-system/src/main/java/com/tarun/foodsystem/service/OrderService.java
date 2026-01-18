package com.tarun.foodsystem.service;

import com.tarun.foodsystem.exception.InvalidOrderStateException;
import com.tarun.foodsystem.exception.OrderCannotBeFulfilledException;
import com.tarun.foodsystem.exception.OrderNotFoundException;
import com.tarun.foodsystem.exception.RestaurantNotFoundException;
import com.tarun.foodsystem.model.*;
import com.tarun.foodsystem.store.InMemoryDataStore;
import com.tarun.foodsystem.strategy.RestaurantSelectionStrategy;
import com.tarun.foodsystem.strategy.StrategyFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Service for managing order operations.
 * Handles order placement, assignment, and completion with concurrency support.
 */
public class OrderService {
    private final InMemoryDataStore dataStore;
    private final ReentrantLock orderLock;

    public OrderService() {
        this.dataStore = InMemoryDataStore.getInstance();
        this.orderLock = new ReentrantLock();
    }

    /**
     * Places a new order and auto-assigns it to a restaurant based on selection criteria.
     * 
     * @param customerName Name of the customer
     * @param items List of items with quantities
     * @param criteria Selection criteria for restaurant
     * @return The placed order
     * @throws OrderCannotBeFulfilledException if no restaurant can fulfill the order
     */
    public Order placeOrder(String customerName, List<OrderItem> items, SelectionCriteria criteria) {
        Order order = new Order(customerName, items, criteria);
        
        orderLock.lock();
        try {
            // Find eligible restaurants (can fulfill all items AND have capacity)
            List<Restaurant> eligibleRestaurants = findEligibleRestaurants(order);
            
            if (eligibleRestaurants.isEmpty()) {
                order.setStatus(OrderStatus.REJECTED);
                dataStore.saveOrder(order);
                
                // Determine the reason for rejection
                List<Restaurant> restaurantsWithItems = dataStore.getAllRestaurants().stream()
                        .filter(r -> r.canFulfillOrder(items))
                        .collect(Collectors.toList());
                
                String reason;
                if (restaurantsWithItems.isEmpty()) {
                    reason = "No restaurant can fulfill all items in the order";
                } else {
                    reason = "All capable restaurants are at full capacity";
                }
                
                throw new OrderCannotBeFulfilledException(reason);
            }

            // Select the best restaurant using the strategy
            RestaurantSelectionStrategy strategy = StrategyFactory.getStrategy(criteria);
            Optional<Restaurant> selectedRestaurant = strategy.selectRestaurant(eligibleRestaurants, order);
            
            if (selectedRestaurant.isEmpty()) {
                order.setStatus(OrderStatus.REJECTED);
                dataStore.saveOrder(order);
                throw new OrderCannotBeFulfilledException("No suitable restaurant found using " + 
                        strategy.getStrategyName() + " strategy");
            }

            Restaurant restaurant = selectedRestaurant.get();
            
            // Try to accept the order (thread-safe)
            if (!restaurant.tryAcceptOrder()) {
                order.setStatus(OrderStatus.REJECTED);
                dataStore.saveOrder(order);
                throw new OrderCannotBeFulfilledException(
                        "Selected restaurant " + restaurant.getName() + " is at full capacity");
            }

            // Calculate and set total cost
            BigDecimal totalCost = restaurant.calculateOrderCost(items)
                    .orElse(BigDecimal.ZERO);
            
            // Update order details
            order.setStatus(OrderStatus.ACCEPTED);
            order.setAssignedRestaurantId(restaurant.getId());
            order.setTotalCost(totalCost);
            
            // Save order and association
            dataStore.saveOrder(order);
            dataStore.addOrderToRestaurant(restaurant.getId(), order.getOrderId());

            System.out.println(String.format("Order %s assigned to %s (Strategy: %s, Cost: Rs.%.2f)",
                    order.getOrderId(), restaurant.getName(), strategy.getStrategyName(), totalCost));
            
            return order;
            
        } finally {
            orderLock.unlock();
        }
    }

    /**
     * Marks an order as completed.
     * 
     * @param restaurantId The restaurant completing the order
     * @param orderId The order to complete
     * @throws InvalidOrderStateException if order is not in ACCEPTED state
     */
    public void markOrderCompleted(String restaurantId, String orderId) {
        orderLock.lock();
        try {
            Order order = dataStore.findOrderById(orderId)
                    .orElseThrow(() -> new OrderNotFoundException(orderId));
            
            Restaurant restaurant = dataStore.findRestaurantById(restaurantId)
                    .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));

            // Validate that this restaurant is assigned to this order
            if (!restaurantId.equals(order.getAssignedRestaurantId())) {
                throw new InvalidOrderStateException(
                        "Order " + orderId + " is not assigned to restaurant " + restaurantId);
            }

            // Validate order status
            if (order.getStatus() != OrderStatus.ACCEPTED) {
                throw new InvalidOrderStateException(
                        "Cannot complete order " + orderId + ". Current status: " + order.getStatus());
            }

            // Update order status
            order.setStatus(OrderStatus.COMPLETED);
            order.setCompletedAt(LocalDateTime.now());
            
            // Release restaurant capacity
            restaurant.releaseOrderSlot();
            
            System.out.println(String.format("Order %s marked as COMPLETED by restaurant %s. " +
                    "Restaurant capacity: %d/%d", 
                    orderId, restaurant.getName(), 
                    restaurant.getCurrentOrders(), restaurant.getMaxCapacity()));
            
        } finally {
            orderLock.unlock();
        }
    }

    /**
     * Gets an order by ID.
     */
    public Order getOrder(String orderId) {
        return dataStore.findOrderById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    /**
     * Gets all orders.
     */
    public List<Order> getAllOrders() {
        return dataStore.getAllOrders();
    }

    /**
     * Gets orders for a specific restaurant.
     */
    public List<Order> getOrdersForRestaurant(String restaurantId) {
        return dataStore.getOrdersForRestaurant(restaurantId).stream()
                .map(orderId -> dataStore.findOrderById(orderId))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * Finds restaurants that can fulfill the order (has all items and has capacity).
     */
    private List<Restaurant> findEligibleRestaurants(Order order) {
        return dataStore.getAllRestaurants().stream()
                .filter(restaurant -> restaurant.canFulfillOrder(order.getItems()))
                .filter(Restaurant::canAcceptOrder)
                .collect(Collectors.toList());
    }

    /**
     * Displays order status.
     */
    public void displayOrderStatus(String orderId) {
        Order order = getOrder(orderId);
        System.out.println("\n=== Order Status ===");
        System.out.println("Order ID: " + order.getOrderId());
        System.out.println("Customer: " + order.getCustomerName());
        System.out.println("Status: " + order.getStatus());
        System.out.println("Items:");
        order.getItems().forEach(item -> System.out.println("  - " + item));
        System.out.println("Selection Criteria: " + order.getSelectionCriteria());
        System.out.println("Assigned Restaurant: " + order.getAssignedRestaurantId());
        System.out.println("Total Cost: Rs." + order.getTotalCost());
        System.out.println("Created At: " + order.getCreatedAt());
        if (order.getCompletedAt() != null) {
            System.out.println("Completed At: " + order.getCompletedAt());
        }
        System.out.println("====================\n");
    }
}
