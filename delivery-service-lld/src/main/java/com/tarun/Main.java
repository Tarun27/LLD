package com.tarun;

import com.tarun.service.DeliveryService;
import com.tarun.service.OnboardingService;
import com.tarun.strategy.OrderCountRankingStrategy;
import com.tarun.strategy.RatingRankingStrategy;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        // Initialize services (they get shared repositories via singleton)
        OnboardingService onboardingService = new OnboardingService();
        DeliveryService deliveryService = new DeliveryService();

        try {
            System.out.println("\n========== Peer-to-Peer Delivery Platform Demo ==========\n");

            // Test Case 1: Onboard customers and drivers
            System.out.println("--- Test Case 1: Onboarding ---");

            // Onboard 10 customers (7 Indian + 3 Western names)
            onboardingService.onboardCustomer("CUST001", "Arjun Sharma");
            onboardingService.onboardCustomer("CUST002", "Priya Patel");
            onboardingService.onboardCustomer("CUST003", "Rahul Mehta");
            onboardingService.onboardCustomer("CUST004", "Ananya Reddy");
            onboardingService.onboardCustomer("CUST005", "Vikram Singh");
            onboardingService.onboardCustomer("CUST006", "Kavya Iyer");
            onboardingService.onboardCustomer("CUST007", "Rohan Gupta");
            onboardingService.onboardCustomer("CUST008", "Emma Watson");
            onboardingService.onboardCustomer("CUST009", "James Miller");
            onboardingService.onboardCustomer("CUST010", "Sophia Anderson");

            // Onboard 2 drivers (Indian names)
            onboardingService.onboardDriver("DRV001", "Amit Kumar");
            onboardingService.onboardDriver("DRV002", "Sneha Desai");

            Thread.sleep(1000);

            // Test Case 1A: Try duplicate onboarding (should fail)
            System.out.println("\n--- Test Case 1A: Duplicate Onboarding  (Error Case) ---");
            boolean dupCustomer = onboardingService.onboardCustomer("CUST001", "Duplicate Person");
            if (!dupCustomer) {
                System.out.println("✓ Expected behavior: Cannot onboard customer with existing ID CUST001");
            }
            boolean dupDriver = onboardingService.onboardDriver("DRV001", "Duplicate Driver");
            if (!dupDriver) {
                System.out.println("✓ Expected behavior: Cannot onboard driver with existing ID DRV001");
            }

            Thread.sleep(1000);

            // Test Case 2: Place initial orders
            System.out.println("\n--- Test Case 2: Placing Initial Orders ---");
            String order1 = deliveryService.placeOrder("CUST001", "ITEM001");
            String order2 = deliveryService.placeOrder("CUST002", "ITEM002");
            String order3 = deliveryService.placeOrder("CUST003", "ITEM003");

            Thread.sleep(3000); // Wait for auto-assignment

            // Test Case 2A: Invalid item order (should fail)
            System.out.println("\n--- Test Case 2A: Invalid Item Order (Error Case) ---");
            String invalidOrder = deliveryService.placeOrder("CUST001", "ITEM999");
            if (invalidOrder == null) {
                System.out.println("✓ Expected behavior: Cannot order non-existent item");
            }

            // Test Case 2B: Invalid customer order (should fail)
            System.out.println("\n--- Test Case 2B: Invalid Customer Order (Error Case) ---");
            String invalidCustomer = deliveryService.placeOrder("CUST999", "ITEM001");
            if (invalidCustomer == null) {
                System.out.println("✓ Expected behavior: Cannot place order for non-existent customer");
            }

            Thread.sleep(1000);

            // Test Case 3: Check order and driver status
            System.out.println("\n--- Test Case 3: Status Check ---");
            System.out.println(deliveryService.showOrderStatus(order1));
            System.out.println(deliveryService.showDriverStatus("DRV001"));

            // Test Case 4: Complete first order
            System.out.println("\n--- Test Case 4: Complete Order Lifecycle ---");
            deliveryService.completeOrder(order1, "CUST001", 4.5);
            System.out.println(deliveryService.showOrderStatus(order1));

            Thread.sleep(1000);

            // Test Case 5: Rating edge cases
            System.out.println("\n--- Test Case 5: Rating Edge Cases ---");

            // Test Case 5A: Try to rate before delivery (should fail)
            String orderEarly = deliveryService.placeOrder("CUST004", "ITEM004");
            Thread.sleep(3000);
            boolean ratingResult = deliveryService.rateDriver(orderEarly, "CUST004", 5.0);
            if (!ratingResult) {
                System.out.println("✓ Expected behavior: Cannot rate before delivery");
            }

            // Test Case 5B: Try to rate with wrong customer (should fail)
            boolean wrongCustomerRating = deliveryService.rateDriver(order1, "CUST002", 3.0);
            if (!wrongCustomerRating) {
                System.out.println("✓ Expected behavior: Only order customer can rate the driver");
            }

            Thread.sleep(1000);

            // Test Case 6: Order Cancellation
            System.out.println("\n--- Test Case 6: Order Cancellation ---");
            System.out.println(deliveryService.showOrderStatus(order3));
            deliveryService.cancelOrder(order3);
            System.out.println(deliveryService.showOrderStatus(order3));

            // Test Case 6A: Try to cancel already cancelled order (should fail)
            boolean cancelAgain = deliveryService.cancelOrder(order3);
            if (!cancelAgain) {
                System.out.println("✓ Expected behavior: Cannot cancel already cancelled order");
            }

            Thread.sleep(1000);

            // Test Case 7: Complete remaining early orders
            System.out.println("\n--- Test Case 7: Completing Multiple Orders ---");
            deliveryService.completeOrder(order2, "CUST002", 5.0);
            deliveryService.completeOrder(orderEarly, "CUST004", 4.0);

            // Test Case 8: Place and complete more orders (total 10 orders)
            System.out.println("\n--- Test Case 8: Processing Remaining Orders ---");
            String order5 = deliveryService.placeOrder("CUST005", "ITEM005");
            deliveryService.completeOrder(order5, "CUST005", 4.8);

            String order6 = deliveryService.placeOrder("CUST006", "ITEM006");
            deliveryService.completeOrder(order6, "CUST006", 4.3);

            String order7 = deliveryService.placeOrder("CUST007", "ITEM007");
            deliveryService.completeOrder(order7, "CUST007", 4.7);

            String order8 = deliveryService.placeOrder("CUST008", "ITEM008");
            deliveryService.completeOrder(order8, "CUST008", 4.6);

            String order9 = deliveryService.placeOrder("CUST009", "ITEM009");
            deliveryService.completeOrder(order9, "CUST009", 4.9);

            String order10 = deliveryService.placeOrder("CUST010", "ITEM010");
            deliveryService.completeOrder(order10, "CUST010", 4.4);

            Thread.sleep(1000);

            // Test Case 9: Dashboard - Top drivers
            System.out.println("\n--- Test Case 9: Top Drivers Dashboard ---");
            System.out.println(deliveryService.showTopDrivers(new OrderCountRankingStrategy(), 10));
            System.out.println(deliveryService.showTopDrivers(new RatingRankingStrategy(), 10));

            // Test Case 10: Try to cancel picked up order (should fail)
            System.out.println("\n--- Test Case 10: Cancel Picked Order (Error Case) ---");
            String order11 = deliveryService.placeOrder("CUST001", "ITEM001");
            String driver11 = deliveryService.waitForDriverAssignment(order11);
            if (driver11 != null) {
                deliveryService.pickupOrder(driver11, order11);
                boolean cancelResult = deliveryService.cancelOrder(order11);
                if (!cancelResult) {
                    System.out.println("✓ Expected behavior: Order cannot be cancelled after pickup");
                }
                deliveryService.deliverOrder(driver11, order11);
            }

            Thread.sleep(1000);

            // Test Case 11: More orders than drivers (queuing system)
            System.out.println("\n--- Test Case 11: Queue System (More Orders than Drivers) ---");
            String order12 = deliveryService.placeOrder("CUST002", "ITEM002");
            String order13 = deliveryService.placeOrder("CUST003", "ITEM003");
            String order14 = deliveryService.placeOrder("CUST004", "ITEM004");
            String order15 = deliveryService.placeOrder("CUST005", "ITEM005");

            System.out.println("Placed 4 orders with only 2 drivers available...");
            Thread.sleep(3000);

            System.out.println("\nOrder Status (showing queue in action):");
            System.out.println(deliveryService.showOrderStatus(order12));
            System.out.println(deliveryService.showOrderStatus(order13));
            System.out.println(deliveryService.showOrderStatus(order14));
            System.out.println(deliveryService.showOrderStatus(order15));

            System.out.println("\nCompleting all queued orders...");
            deliveryService.completeOrder(order12, "CUST002", 4.5);
            deliveryService.completeOrder(order13, "CUST003", 4.8);
            deliveryService.completeOrder(order14, "CUST004", 4.2);
            deliveryService.completeOrder(order15, "CUST005", 4.7);

            System.out.println("\n✓ Queue demonstration complete: All orders successfully processed!");

            Thread.sleep(1000);

            // Final System Summary
            System.out.println("\n--- Final System Summary ---");
            deliveryService.printSystemSummary();

            System.out.println("\n========== Demo Completed Successfully ==========\n");

        } catch (Exception e) {
            System.err.println("\n!!! Demo failed with error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            deliveryService.shutdown();
        }
    }
}