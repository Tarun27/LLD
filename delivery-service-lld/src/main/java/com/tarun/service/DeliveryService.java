package com.tarun.service;


import com.tarun.model.*;
import com.tarun.repository.CustomerRepository;
import com.tarun.repository.DriverRepository;
import com.tarun.repository.ItemRepository;
import com.tarun.repository.OrderRepository;
import com.tarun.strategy.DriverRankingStrategy;

import java.time.LocalDateTime;
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

    /**
     * Creates DeliveryService, initializes repositories, items, and starts background workers
     */
    public DeliveryService() {
        this.customerRepository = CustomerRepository.getInstance();
        this.driverRepository = DriverRepository.getInstance();
        this.itemRepository = new ItemRepository();
        this.orderRepository = new OrderRepository();
        this.notificationService = new NotificationServiceImpl();
        initializeItems();
        startDriverAssignmentWorker();
    }

    /**
     * Initializes the system with predefined items that can be delivered
     */
    private void initializeItems() {
        itemRepository.save(new Item("ITEM001", "Documents", "Important documents"));
        itemRepository.save(new Item("ITEM002", "Food", "Food package"));
        itemRepository.save(new Item("ITEM003", "Electronics", "Electronic items"));
        itemRepository.save(new Item("ITEM004", "Medicines", "Medical supplies"));
        itemRepository.save(new Item("ITEM005", "Groceries", "Grocery items"));
        itemRepository.save(new Item("ITEM006", "Flowers", "Fresh flower bouquet"));
        itemRepository.save(new Item("ITEM007", "Books", "Books and magazines"));
        itemRepository.save(new Item("ITEM008", "Clothes", "Clothing items"));
        itemRepository.save(new Item("ITEM009", "Gifts", "Gift packages"));
        itemRepository.save(new Item("ITEM010", "Stationery", "Office supplies"));
    }

    /**
     * Places a new order for delivery
     * Validates customer and item existence
     * Schedules auto-cancellation after 30 minutes if not picked up
     * Returns order ID if successful, null if validation fails
     */
    public String placeOrder(String customerId, String itemId) {
        try {
            if (!customerRepository.exists(customerId)) {
                return null;
            }
            if (!itemRepository.exists(itemId)) {
                return null;
            }

            String orderId = "ORD" + String.format("%05d", orderIdCounter.getAndIncrement());
            Order order = new Order(orderId, customerId, itemId);
            orderRepository.save(order);

            System.out.println("Order placed: " + orderId + " by customer " + customerId + " for item " + itemId);
            notificationService.sendEmail(customerId, "Order Placed", "Your order " + orderId + " has been placed successfully.");

            // Schedule auto-cancellation after 30 minutes
            scheduleOrderCancellation(order);

            return orderId;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to place order: " + e.getMessage());
            return null;
        }
    }

    /**
     * Schedules automatic cancellation of an order after 30 minutes
     * Cancels only if order hasn't been picked up, delivered, or manually cancelled
     */
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

    /**
     * Cancels an order
     * Cannot cancel if order is already picked up, delivered, or cancelled
     * Frees up assigned driver if any
     * Returns true if cancellation successful, false otherwise
     */
    public boolean cancelOrder(String orderId) {
        try {
            Order order = orderRepository.findById(orderId);
            if (order == null) {
                return false;
            }

            synchronized (order) {
                if (order.getStatus() == OrderStatus.PICKED_UP) {
                    return false;
                }

                if (order.getStatus() == OrderStatus.CANCELLED) {
                    return false;
                }

                if (order.getStatus() == OrderStatus.DELIVERED) {
                    return false;
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
                return true;
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to cancel order: " + e.getMessage());
            return false;
        }
    }

    /**
     * Marks an order as picked up by a driver
     * Validates driver and order exist, order is not cancelled
     * Cancels the 30-minute auto-cancellation timer
     * Returns true if pickup successful, false otherwise
     */
    public boolean pickupOrder(String driverId, String orderId) {
        try {
            Driver driver = driverRepository.findById(driverId);
            if (driver == null) {
                return false;
            }

            Order order = orderRepository.findById(orderId);
            if (order == null) {
                return false;
            }

            synchronized (order) {
                if (order.getStatus() == OrderStatus.CANCELLED) {
                    return false;
                }

                if (!orderId.equals(order.getAssignedDriverId()) && order.getAssignedDriverId() != null
                        && !driverId.equals(order.getAssignedDriverId())) {
                    return false;
                }

                if (order.getStatus() == OrderStatus.PICKED_UP) {
                    return false;
                }

                if (order.getStatus() == OrderStatus.DELIVERED) {
                    return false;
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
                return true;
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to pickup order: " + e.getMessage());
            return false;
        }
    }

    /**
     * Marks an order as delivered
     * Validates order was picked up by this driver
     * Makes driver available for new orders and increments completed order count
     * Returns true if delivery successful, false otherwise
     */
    public boolean deliverOrder(String driverId, String orderId) {
        try {
            Driver driver = driverRepository.findById(driverId);
            if (driver == null) {
                return false;
            }

            Order order = orderRepository.findById(orderId);
            if (order == null) {
                return false;
            }

            synchronized (order) {
                if (!driverId.equals(order.getAssignedDriverId())) {
                    return false;
                }

                if (order.getStatus() != OrderStatus.PICKED_UP) {
                    return false;
                }

                order.setStatus(OrderStatus.DELIVERED);
                order.setDeliveredAt(LocalDateTime.now());
                driver.setAvailable(true);
                driver.incrementCompletedOrders();

                System.out.println("Order delivered: " + orderId + " by driver " + driverId);
                notificationService.sendEmail(order.getCustomerId(), "Order Delivered",
                        "Your order " + orderId + " has been delivered successfully.");
                notificationService.sendSMS(driverId, "Order " + orderId + " marked as delivered");
                return true;
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to deliver order: " + e.getMessage());
            return false;
        }
    }

    /**
     * Allows customer to rate a driver after delivery
     * Validates rating is between 1.0 and 5.0, order is delivered, and customer owns the order
     * Returns true if rating successful, false otherwise
     */
    public boolean rateDriver(String orderId, String customerId, double rating) {
        try {
            if (rating < 1.0 || rating > 5.0) {
                return false;
            }

            Order order = orderRepository.findById(orderId);
            if (order == null) {
                return false;
            }

            if (!customerId.equals(order.getCustomerId())) {
                return false;
            }

            synchronized (order) {
                if (order.getStatus() != OrderStatus.DELIVERED) {
                    return false;
                }

                String driverId = order.getAssignedDriverId();
                if (driverId == null) {
                    return false;
                }

                Driver driver = driverRepository.findById(driverId);
                if (driver != null) {
                    driver.addRating(rating);
                    System.out.println("Driver " + driverId + " rated " + rating + " stars for order " + orderId);
                    notificationService.sendSMS(driverId, "You received a " + rating + " star rating");
                    return true;
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            System.err.println("ERROR: Failed to rate driver: " + e.getMessage());
            return false;
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

    public String showTopDrivers(DriverRankingStrategy strategy, int limit) {
        List<Driver> allDrivers = driverRepository.getAllDrivers();
        List<Driver> rankedDrivers = strategy.rankDrivers(allDrivers);

        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Top Drivers ===\n");
        int rank = 1;
        for (Driver driver : rankedDrivers) {
            if (rank > limit) break;
            // Only show drivers with completed orders
            if (driver.getCompletedOrders() > 0) {
                sb.append(rank++).append(". ").append(driver.getName())
                        .append(" (ID: ").append(driver.getId()).append(")")
                        .append(" - Orders: ").append(driver.getCompletedOrders())
                        .append(", Rating: ").append(String.format("%.2f", driver.getAverageRating()))
                        .append("\n");
            }
        }
        if (rank == 1) {
            sb.append("No drivers with completed orders yet.\n");
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

    /**
     * Returns the ID of the driver assigned to an order, or null if not assigned
     */
    public String getAssignedDriver(String orderId) {
        Order order = orderRepository.findById(orderId);
        return order != null ? order.getAssignedDriverId() : null;
    }

    /**
     * Waits for an order to be assigned to a driver
     * Polls every 2 seconds for up to 5 minutes
     * Returns null if order is cancelled during wait or timeout occurs
     */
    public String waitForDriverAssignment(String orderId) {
        while (true) {
            String driverId = getAssignedDriver(orderId);
            if (driverId != null) {
                return driverId;
            }
            try {
                Thread.sleep(2000); // Check every 2 seconds (same as auto-assignment interval)
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("ERROR: Interrupted while waiting for driver assignment");
                return null;
            }
        }
    }

    public void printSystemSummary() {
        System.out.println("\n========== System Summary ==========");

        List<Driver> allDrivers = driverRepository.getAllDrivers();
        int totalDrivers = allDrivers.size();
        int activeDrivers = (int) allDrivers.stream().filter(d -> !d.isAvailable()).count();
        int totalDeliveries = allDrivers.stream().mapToInt(Driver::getCompletedOrders).sum();

        System.out.println("Total Drivers: " + totalDrivers);
        System.out.println("Currently Active (delivering): " + activeDrivers);
        System.out.println("Total Completed Deliveries: " + totalDeliveries);

        List<Order> pendingOrders = orderRepository.findPendingOrders();
        List<Order> assignedOrders = orderRepository.findAssignedOrders();
        List<Order> cancelledOrders = orderRepository.findCancelledOrders();

        System.out.println("Cancelled Orders: " + cancelledOrders.size());
        System.out.println("Pending Orders: " + pendingOrders.size());
        System.out.println("Assigned Orders: " + assignedOrders.size());
        System.out.println("====================================\n");
    }

    /**
     * Completes the full order lifecycle: waits for assignment, pickup, delivery, and rating
     * Returns true if all steps complete successfully, false otherwise
     */
    public boolean completeOrder(String orderId, String customerId, double rating) {
        try {
            // Wait for driver assignment
            String driverId = waitForDriverAssignment(orderId);
            if (driverId == null) {
                System.err.println("ERROR: Could not assign driver to order " + orderId);
                return false;
            }

            // Pickup order
            if (!pickupOrder(driverId, orderId)) {
                return false;
            }

            // Deliver order
            if (!deliverOrder(driverId, orderId)) {
                return false;
            }

            // Rate driver
            if (!rateDriver(orderId, customerId, rating)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            System.err.println("ERROR: Failed to complete order " + orderId + ": " + e.getMessage());
            return false;
        }
    }
}
