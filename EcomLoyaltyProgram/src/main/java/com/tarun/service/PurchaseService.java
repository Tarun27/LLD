package com.tarun.service;

import com.tarun.model.PurchaseRequest;
import com.tarun.model.PurchaseResult;
import com.tarun.model.User;
import com.tarun.strategies.DiscountStrategy;
import com.tarun.strategies.PointsCalculationStrategy;
import com.tarun.strategies.RedemptionValidator;
import com.tarun.strategies.impl.DefaultPointsCalculationStrategy;
import com.tarun.strategies.impl.PersonalizedDiscountStrategy;
import com.tarun.strategies.impl.PointsRedemptionValidator;

public class PurchaseService {
    private final UserService userService;
    private final RedemptionValidator redemptionValidator;
    private final PointsCalculationStrategy pointsCalculationStrategy;
    private final DiscountStrategy discountStrategy;

    public PurchaseService(UserService userService) {
        this.userService = userService;
        this.redemptionValidator = new PointsRedemptionValidator();
        this.pointsCalculationStrategy = new DefaultPointsCalculationStrategy();
        this.discountStrategy = new PersonalizedDiscountStrategy();
    }

    public PurchaseResult purchase(PurchaseRequest request) {
        User user = userService.getUser(request.getUserName());
        if (user == null) {
            return PurchaseResult.builder()
                    .success(false)
                    .message("User not found: " + request.getUserName())
                    .build();
        }

        // Validate redemption
        if (!redemptionValidator.validate(user, request.getOrderAmount(), request.getPointsToRedeem())) {
            return  PurchaseResult.builder()
                    .success(false)
                    .message(redemptionValidator.getErrorMessage())
                    .build();
        }

        // Calculate amount after redemption
        double amountAfterRedemption = request.getOrderAmount() - request.getPointsToRedeem();

        // Calculate and apply discount
        double discount = discountStrategy.calculateDiscount(user, amountAfterRedemption);
        double amountAfterDiscount = amountAfterRedemption - discount;

        // Calculate points earned on remaining amount
        double pointsEarned = pointsCalculationStrategy.calculatePoints(
                amountAfterDiscount, user.getLevel());

        // Update user state
        user.deductPoints(request.getPointsToRedeem());
        user.addPoints(pointsEarned);
        user.incrementOrderCount();
        user.addToTotalSpent(request.getOrderAmount());

        // Reset discount eligibility if discount was applied
        if (discount > 0) {
            user.markDiscountUsed();
        }

        return  PurchaseResult.builder()
                .success(true)
                .pointsRedeemed(request.getPointsToRedeem())
                .pointsAdded(pointsEarned)
                .discountApplied(discount)
                .totalPayable(amountAfterDiscount)
                .currentPoints(user.getPoints())
                .currentLevel(user.getLevel())
                .build();
    }
}
