package com.tarun;

import com.tarun.model.PurchaseRequest;
import com.tarun.model.PurchaseResult;
import com.tarun.service.PurchaseService;
import com.tarun.service.UserService;


public class LoyaltyProgramApplication {
    private final UserService userService;
    private final PurchaseService purchaseService;

    public LoyaltyProgramApplication() {
        this.userService = new UserService();
        this.purchaseService = new PurchaseService(userService);
    }

    public void onboardUser(String userName) {
        userService.onboardUser(userName);
    }

    public void purchase(String userName, double orderAmount, double pointsToRedeem) {
        PurchaseRequest request = new PurchaseRequest(userName, orderAmount, pointsToRedeem);
        PurchaseResult result = purchaseService.purchase(request);
        System.out.println(result);
    }

    public void getUserStats(String userName) {
        System.out.println(userService.getUserStats(userName));
    }

    public static void main(String[] args) {
        LoyaltyProgramApplication app = new LoyaltyProgramApplication();

        // Test Cases
        System.out.println("=== Test Case 1: Onboard User ===");
        app.onboardUser("user1");
        System.out.println();

        System.out.println("=== Test Case 2: Bronze Purchase (No Redemption) ===");
        app.purchase("user1", 800.00, 0);
        System.out.println();

        System.out.println("=== Test Case 3: Bronze Purchase (Insufficient Points) ===");
        app.purchase("user1", 4200.00, 100);
        System.out.println();

        System.out.println("=== Test Case 4: Bronze Purchase (Approaching Silver) ===");
        app.purchase("user1", 4200.00, 0);
        System.out.println();

        System.out.println("=== Test Case 5: Silver Purchase (With Redemption) ===");
        app.purchase("user1", 3000.00, 300);
        System.out.println();

        System.out.println("=== Test Case 6: Silver Purchase (Approaching Gold) ===");
        app.purchase("user1", 5000.00, 0);
        System.out.println();

        System.out.println("=== Test Case 7: Gold Purchase (With Redemption & Discount) ===");
        app.purchase("user1", 12000.00, 800);
        System.out.println();

        System.out.println("=== Test Case 8: Get User Stats ===");
        app.getUserStats("user1");
        System.out.println();

        // Additional test cases
        System.out.println("=== Additional Test: User Not Found ===");
        app.purchase("user2", 1000, 0);
        System.out.println();

        System.out.println("=== Additional Test: Multiple Users ===");
        app.onboardUser("user2");
        app.purchase("user2", 10000, 0);
        app.getUserStats("user2");
    }
}