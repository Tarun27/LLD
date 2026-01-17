package com.tarun.model;

import lombok.Data;

@Data
public class User {

        private final String name;
        private double points;
        private UserLevel level;
        private int orderCount;
        private double totalSpent;
        private int ordersAtLastDiscount;     // NEW: Track when discount was used
        private double spentAtLastDiscount;   // NEW: Track spending at discount

        public User(String name) {
            this.name = name;
            this.points = 0;
            this.level = UserLevel.BRONZE;
            this.orderCount = 0;
            this.totalSpent = 0;
            this.ordersAtLastDiscount = 0;
            this.spentAtLastDiscount = 0;
        }

        public boolean isDiscountEligible() {
            // Check if user has earned NEW eligibility since last discount
            int newOrders = this.orderCount - this.ordersAtLastDiscount;
            double newSpent = this.totalSpent - this.spentAtLastDiscount;

            boolean hasMinOrders = newOrders > 3;
            boolean hasMinSpent = newSpent > 10000.0;

            return hasMinOrders || hasMinSpent;
        }

        public void markDiscountUsed() {
            // Record current state when discount is applied
            this.ordersAtLastDiscount = this.orderCount;
            this.spentAtLastDiscount = this.totalSpent;
        }

    public void addPoints(double points) {
        this.points += points;
        updateLevel();
    }

    public void deductPoints(double points) {
        this.points -= points;
    }

    public void incrementOrderCount() {
        this.orderCount++;
    }

    public void addToTotalSpent(double amount) {
        this.totalSpent += amount;
    }


    public void updateLevel() {
        this.level = UserLevel.getLevelByPoints(this.points);
    }


}
