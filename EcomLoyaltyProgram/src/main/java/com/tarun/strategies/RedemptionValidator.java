package com.tarun.strategies;

import com.tarun.model.User;

public interface RedemptionValidator {
    boolean validate(User user, double orderAmount, double pointsToRedeem);
    String getErrorMessage();
}
