package com.tarun.strategies.impl;

import com.tarun.model.User;
import com.tarun.model.UserLevel;
import com.tarun.strategies.RedemptionValidator;

public class PointsRedemptionValidator implements RedemptionValidator {
    private String errorMessage;

    @Override
    public boolean validate(User user, double orderAmount, double pointsToRedeem) {
        if (pointsToRedeem > user.getPoints()) {
            errorMessage = "Purchase Failed. Not enough points to redeem";
            return false;
        }

        UserLevel level = user.getLevel();
        double maxRedeemableByPercentage = orderAmount * level.getMaxRedemptionPercentage();
        double maxRedeemable = Math.min(maxRedeemableByPercentage, level.getMaxRedeemablePoints());

        if (pointsToRedeem > maxRedeemable) {
            errorMessage = String.format("Purchase Failed. Maximum redeemable points: %.1f", maxRedeemable);
            return false;
        }

        return true;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }
}
