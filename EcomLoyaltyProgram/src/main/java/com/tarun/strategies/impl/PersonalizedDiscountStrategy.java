package com.tarun.strategies.impl;

import com.tarun.model.User;
import com.tarun.strategies.DiscountStrategy;

public class PersonalizedDiscountStrategy implements DiscountStrategy {
    private static final int MIN_ORDERS_FOR_DISCOUNT = 3;
    private static final double MIN_SPENT_FOR_DISCOUNT = 10000.0;
    private static final double ORDERS_DISCOUNT = 0.05;
    private static final double SPENT_DISCOUNT = 0.10;
    private static final double COMBINED_DISCOUNT = 0.12;
    private static final double MAX_DISCOUNT = 5000.0;

    @Override
    public double calculateDiscount(User user, double amount) {
        if (!user.isDiscountEligible()) {
            return 0;
        }

        // Now uses the delta calculation
        int newOrders = user.getOrderCount() - user.getOrdersAtLastDiscount();
        double newSpent = user.getTotalSpent() - user.getSpentAtLastDiscount();

        boolean hasMinOrders = newOrders > MIN_ORDERS_FOR_DISCOUNT;
        boolean hasMinSpent = newSpent > MIN_SPENT_FOR_DISCOUNT;

        double discountPercentage = 0;
        if (hasMinOrders && hasMinSpent) {
            discountPercentage = COMBINED_DISCOUNT;
        } else if (hasMinSpent) {
            discountPercentage = SPENT_DISCOUNT;
        } else if (hasMinOrders) {
            discountPercentage = ORDERS_DISCOUNT;
        }

        double discount = amount * discountPercentage;
        return Math.min(discount, MAX_DISCOUNT);
    }
}
