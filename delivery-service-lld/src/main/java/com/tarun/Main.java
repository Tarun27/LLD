package com.tarun;

import com.tarun.service.DeliveryService;
import com.tarun.strategy.OrderCountRankingStrategy;
import com.tarun.strategy.RatingRankingStrategy;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) throws InterruptedException {
        DeliveryService service = new DeliveryService();

        try {
            System.out.println("\n========== Peer-to-Peer Delivery Platform Demo ==========\n");

            // Test Case 1: Onboard customers and drivers
            System.out.println("--- Test Case 1: Onboarding ---");
            service.onboardCustomer("CUST001", "Alice Johnson");
            service.onboardCustomer("CUST002", "Bob Smith");
            service.onboardCustomer("CUST003", "Charlie Brown");

            service.onboardDriver("DRV001", "David Lee");
            service.onboardDriver("DRV002", "Emma Wilson");

            Thread.sleep(1000);

            // Test Case 2: Place orders
            System.out.println("\n--- Test Case 2: Placing Orders ---");
            String order1 = service.placeOrder("CUST001", "ITEM001");
            String order2 = service.placeOrder("CUST002", "ITEM002");
            String order3 = service.placeOrder("CUST003", "ITEM003");

            Thread.sleep(3000); // Wait for auto-assignment

            // Test Case 3: Check order and driver status
            System.out.println("\n--- Test Case 3: Status Check ---");
            System.out.println(service.showOrderStatus(order1));
            System.out.println(service.showDriverStatus("DRV001"));

            // Test Case 4: Pickup and deliver
            System.out.println("\n--- Test Case 4: Pickup and Delivery ---");
            service.pickupOrder("DRV001", order1);
            System.out.println(service.showOrderStatus(order1));

            service.deliverOrder("DRV001", order1);
            System.out.println(service.showOrderStatus(order1));
            System.out.println(service.showDriverStatus("DRV001"));

            Thread.sleep(2000); // Wait for order3 to be assigned

            // Test Case 5: Rate driver
            System.out.println("\n--- Test Case 5: Rating ---");
            service.rateDriver(order1, "CUST001", 4.5);
            System.out.println(service.showDriverStatus("DRV001"));

            // Test Case 6: Cancel order before pickup
            System.out.println("\n--- Test Case 6: Order Cancellation ---");
            System.out.println(service.showOrderStatus(order3));
            service.cancelOrder(order3);
            System.out.println(service.showOrderStatus(order3));

            // Test Case 7: Complete more orders for dashboard
            System.out.println("\n--- Test Case 7: Multiple Orders for Dashboard ---");
            service.pickupOrder("DRV002", order2);
            service.deliverOrder("DRV002", order2);
            service.rateDriver(order2, "CUST002", 5.0);

            String order4 = service.placeOrder("CUST001", "ITEM004");
            Thread.sleep(3000);
            service.pickupOrder("DRV001", order4);
            service.deliverOrder("DRV001", order4);
            service.rateDriver(order4, "CUST001", 4.0);

            String order5 = service.placeOrder("CUST002", "ITEM005");
            Thread.sleep(3000);
            service.pickupOrder("DRV002", order5);
            service.deliverOrder("DRV002", order5);
            service.rateDriver(order5, "CUST002", 4.8);

            // Test Case 8: Dashboard - Top drivers
            System.out.println("\n--- Test Case 8: Top Drivers Dashboard ---");
            System.out.println(service.showTopDrivers(new OrderCountRankingStrategy()));
            System.out.println(service.showTopDrivers(new RatingRankingStrategy()));

            // Test Case 9: Try to cancel picked up order (should fail)
            System.out.println("\n--- Test Case 9: Cancel Picked Order (Error Case) ---");
            String order6 = service.placeOrder("CUST003", "ITEM001");
            Thread.sleep(3000);
            service.pickupOrder("DRV001", order6);
            try {
                service.cancelOrder(order6);
            } catch (IllegalStateException e) {
                System.out.println("Expected error: " + e.getMessage());
            }
            service.deliverOrder("DRV001", order6);

            // Test Case 10: More orders than drivers
            System.out.println("\n--- Test Case 10: More Orders than Drivers ---");
            String order7 = service.placeOrder("CUST001", "ITEM002");
            String order8 = service.placeOrder("CUST002", "ITEM003");
            String order9 = service.placeOrder("CUST003", "ITEM004");

            Thread.sleep(2000);
            System.out.println(service.showOrderStatus(order7));
            System.out.println(service.showOrderStatus(order8));
            System.out.println(service.showOrderStatus(order9));

            System.out.println("\n========== Demo Completed ==========\n");

        } finally {
            service.shutdown();
        }
    }
}