package com.tarun.strategies;

import com.tarun.model.User;

public interface DiscountStrategy {

    double calculateDiscount(User user, double amount);

}
