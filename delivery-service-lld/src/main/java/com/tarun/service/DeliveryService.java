package com.tarun.service;


import com.tarun.model.*;
import com.tarun.repository.CustomerRepository;
import com.tarun.repository.DriverRepository;
import com.tarun.repository.ItemRepository;
import com.tarun.repository.OrderRepository;
import com.tarun.strategy.DriverRankingStrategy;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class DeliveryService {
    private final CustomerRepository customerRepository;
    private final DriverRepository driverRepository;
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final AtomicInteger orderIdCounter = new AtomicInteger(1);
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> orderCancellationTasks = new ConcurrentHashMap<>();

    public DeliveryService() {
        this.customerRepository = new CustomerRepository();
        this.driverRepository = new DriverRepository();
        this.itemRepository = new ItemRepository();
        this.orderRepository = new OrderRepository();
        this.notificationService = new NotificationServiceImpl();
        initializeItems();
        startDriverAssignmentWorker();
    }

    private void initializeItems() {
        itemRepository.save(new Item("ITEM001", "Documents", "Important documents"));
        itemRepository.save(new Item("ITEM002", "Food", "Food package"));
        itemRepository.save(new Item("ITEM003", "Electronics", "Electronic items"));
        itemRepository.save(new Item("ITEM004", "Medicines", "Medical supplies"));
        itemRepository.save(new Item("ITEM005", "Groceries", "Grocery items"));
    }

    public void onboardCustomer(String id, String name) {
        if (customerRepository.exists(id)) {
            throw new IllegalArgumentException("Customer with ID " + id + " already exists");
        }
        Customer customer = new Customer(id, name);
        customerRepository.save(customer);
        System.out.println("Customer onboarded: " + id + " - " + name);
    }

    public void onboardDriver(String id, String name) {
        if (driverRepository.exists(id)) {
            throw new IllegalArgumentException("Driver with ID " + id + " already exists");
        }
        Driver driver = new Driver(id, name);
        driverRepository.save(driver);
        System.out.println("Driver onboarded: " + id + " - " + name);
    }

    public String placeOrder(String customerId, String itemId) {
        if (!customerRepository.exists(customerId)) {
            throw new IllegalArgumentException("Customer not found: " + customerId);
        }
        if (!itemRepository.exists(itemId)) {
            throw new IllegalArgumentException("Item not found: " + itemId);
        }

        String orderId = "ORD" + String.format("%05d", orderIdCounter.getAndIncrement());
        Order order = new Order(orderId, customerId, itemId);
        orderRepository.save(order);

        System.out.println("Order placed: " + orderId + " by customer " + customerId + " for item " + itemId);
        notificationService.sendEmail(customerId, "Order Placed", "Your order " + orderId + " has been placed successfully.");

        // Schedule auto-cancellation after 30 minutes
        scheduleOrderCancellation(order);

        return orderId;
    }

    private void scheduleOrderCancellation(Order order) {
        ScheduledFuture<?> task = scheduler.schedule(() -> {
            synchronized (order) {
                if (order.getStatus() != OrderStatus.PICKED_UP &&
                        order.getStatus() != OrderStatus.DELIVERED &&
                        order.getStatus() != OrderStatus.CANCELLED) {

                    order.setStatus(OrderStatus.CANCELLED);
                    System.out.println("Order " + order.getId() + " auto-cancelled due to 30 minute timeout");

                    String driverId = order.getAssignedDriverId();
                    if (driverId != null) {
                        Driver driver = driverRepository.findById(driverId);
                        if (driver != null) {
                            driver.setAvailable(true);
                            notificationService.sendSMS(driverId, "Order " + order.getId() + " cancelled (timeout)");
                        }
                    }

                    notificationService.sendEmail(order.getCustomerId(), "Order Cancelled",
                            "Order " + order.getId() + " was cancelled due to no driver pickup within 30 minutes.");
                }
            }
        }, 30, TimeUnit.MINUTES);

        orderCancellationTasks.put(order.getId(), task);
    }

    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        synchronized (order) {
            if (order.getStatus() == OrderStatus.PICKED_UP) {
                throw new IllegalStateException("Cannot cancel order that has been picked up");
            }

            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new IllegalStateException("Order is already cancelled");
            }

            if (order.getStatus() == OrderStatus.DELIVERED) {
                throw new IllegalStateException("Cannot cancel delivered order");
            }

            order.setStatus(OrderStatus.CANCELLED);
            System.out.println("Order cancelled: " + orderId);

            // Cancel the auto-cancellation task
            ScheduledFuture<?> task = orderCancellationTasks.remove(orderId);
            if (task != null) {
                task.cancel(false);
            }

            String driverId = order.getAssignedDriverId();
            if (driverId != null) {
                Driver driver = driverRepository.findById(driverId);
                if (driver != null) {
                    driver.setAvailable(true);
                    notificationService.sendSMS(driverId, "Order " + orderId + " has been cancelled");
                }
            }

            notificationService.sendEmail(order.getCustomerId(), "Order Cancelled",
                    "Your order " + orderId + " has been cancelled.");
        }
    }

    public void pickupOrder(String driverId, String orderId) {
        Driver driver = driverRepository.findById(driverId);
        if (driver == null) {
            throw new IllegalArgumentException("Driver not found: " + driverId);
        }

        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        synchronized (order) {
            if (order.getStatus() == OrderStatus.CANCELLED) {
                throw new IllegalStateException("Cannot pickup cancelled order");
            }

            if (!orderId.equals(order.getAssignedDriverId()) && order.getAssignedDriverId() != null
                    && !driverId.equals(order.getAssignedDriverId())) {
                throw new IllegalStateException("Order is assigned to different driver");
            }

            if (order.getStatus() == OrderStatus.PICKED_UP) {
                throw new IllegalStateException("Order already picked up");
            }

            if (order.getStatus() == OrderStatus.DELIVERED) {
                throw new IllegalStateException("Order already delivered");
            }

            order.setStatus(OrderStatus.PICKED_UP);
            order.setPickedUpAt(LocalDateTime.now());
            order.setAssignedDriverId(driverId);

            // Cancel the auto-cancellation task
            ScheduledFuture<?> task = orderCancellationTasks.remove(orderId);
            if (task != null) {
                task.cancel(false);
            }

            System.out.println("Order picked up: " + orderId + " by driver " + driverId);
            notificationService.sendEmail(order.getCustomerId(), "Order Picked Up",
                    "Your order " + orderId + " has been picked up by driver " + driverId);
        }
    }

    public void deliverOrder(String driverId, String orderId) {
        Driver driver = driverRepository.findById(driverId);
        if (driver == null) {
            throw new IllegalArgumentException("Driver not found: " + driverId);
        }

        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        synchronized (order) {
            if (!driverId.equals(order.getAssignedDriverId())) {
                throw new IllegalStateException("Order not assigned to this driver");
            }

            if (order.getStatus() != OrderStatus.PICKED_UP) {
                throw new IllegalStateException("Order must be picked up before delivery");
            }

            order.setStatus(OrderStatus.DELIVERED);
            order.setDeliveredAt(LocalDateTime.now());
            driver.setAvailable(true);
            driver.incrementCompletedOrders();

            System.out.println("Order delivered: " + orderId + " by driver " + driverId);
            notificationService.sendEmail(order.getCustomerId(), "Order Delivered",
                    "Your order " + orderId + " has been delivered successfully.");
            notificationService.sendSMS(driverId, "Order " + orderId + " marked as delivered");
        }
    }

    public void rateDriver(String orderId, String customerId, double rating) {
        if (rating < 1.0 || rating > 5.0) {
            throw new IllegalArgumentException("Rating must be between 1.0 and 5.0");
        }

        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }

        if (!customerId.equals(order.getCustomerId())) {
            throw new IllegalArgumentException("Only the customer who placed the order can rate");
        }

        synchronized (order) {
            if (order.getStatus() != OrderStatus.DELIVERED) {
                throw new IllegalStateException("Can only rate after order is delivered");
            }

            String driverId = order.getAssignedDriverId();
            if (driverId == null) {
                throw new IllegalStateException("No driver assigned to this order");
            }

            Driver driver = driverRepository.findById(driverId);
            if (driver != null) {
                driver.addRating(rating);
                System.out.println("Driver " + driverId + " rated " + rating + " stars for order " + orderId);
                notificationService.sendSMS(driverId, "You received a " + rating + " star rating");
            }
        }
    }

    public String showOrderStatus(String orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            return "Order not found: " + orderId;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Order Status ===\n");
        sb.append("Order ID: ").append(order.getId()).append("\n");
        sb.append("Customer ID: ").append(order.getCustomerId()).append("\n");
        sb.append("Item ID: ").append(order.getItemId()).append("\n");
        sb.append("Status: ").append(order.getStatus()).append("\n");
        sb.append("Created At: ").append(order.getCreatedAt()).append("\n");
        sb.append("Assigned Driver: ").append(order.getAssignedDriverId() != null ? order.getAssignedDriverId() : "Not assigned").append("\n");
        if (order.getPickedUpAt() != null) {
            sb.append("Picked Up At: ").append(order.getPickedUpAt()).append("\n");
        }
        if (order.getDeliveredAt() != null) {
            sb.append("Delivered At: ").append(order.getDeliveredAt()).append("\n");
        }
        sb.append("===================\n");

        return sb.toString();
    }

    public String showDriverStatus(String driverId) {
        Driver driver = driverRepository.findById(driverId);
        if (driver == null) {
            return "Driver not found: " + driverId;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Driver Status ===\n");
        sb.append("Driver ID: ").append(driver.getId()).append("\n");
        sb.append("Name: ").append(driver.getName()).append("\n");
        sb.append("Available: ").append(driver.isAvailable() ? "Yes" : "No").append("\n");
        sb.append("Completed Orders: ").append(driver.getCompletedOrders()).append("\n");
        sb.append("Average Rating: ").append(String.format("%.2f", driver.getAverageRating())).append("\n");
        sb.append("Total Ratings: ").append(driver.getRatingCount()).append("\n");
        sb.append("====================\n");

        return sb.toString();
    }

    public String showTopDrivers(DriverRankingStrategy strategy) {
        List<Driver> allDrivers = driverRepository.getAllDrivers();
        List<Driver> rankedDrivers = strategy.rankDrivers(allDrivers);

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Top Drivers ===\n");
        int rank = 1;
        for (Driver driver : rankedDrivers) {
            if (rank > 10) break; // Show top 10
            sb.append(rank++).append(". ").append(driver.getName())
                    .append(" (ID: ").append(driver.getId()).append(")")
                    .append(" - Orders: ").append(driver.getCompletedOrders())
                    .append(", Rating: ").append(String.format("%.2f", driver.getAverageRating()))
                    .append("\n");
        }
        sb.append("==================\n");

        return sb.toString();
    }

    private void startDriverAssignmentWorker() {
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                assignPendingOrders();
            } catch (Exception e) {
                System.err.println("Error in driver assignment worker: " + e.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void assignPendingOrders() {
        List<Order> pendingOrders = orderRepository.findPendingOrders();
        List<Driver> availableDrivers = driverRepository.findAvailableDrivers();

        for (Order order : pendingOrders) {
            synchronized (order) {
                if (order.getStatus() != OrderStatus.PENDING) {
                    continue;
                }

                for (Driver driver : availableDrivers) {
                    synchronized (driver) {
                        if (driver.isAvailable()) {
                            order.setStatus(OrderStatus.ASSIGNED);
                            order.setAssignedDriverId(driver.getId());
                            driver.setAvailable(false);

                            System.out.println("Order " + order.getId() + " assigned to driver " + driver.getId());
                            notificationService.sendSMS(driver.getId(), "New order assigned: " + order.getId());
                            notificationService.sendEmail(order.getCustomerId(), "Order Assigned",
                                    "Your order " + order.getId() + " has been assigned to driver " + driver.getId());
                            break;
                        }
                    }
                }
            }
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}
