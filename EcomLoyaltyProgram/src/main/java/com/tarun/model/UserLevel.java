package com.tarun.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum UserLevel {
    BRONZE(0, 499, 10.0, 0.05, 200),
    SILVER(500, 999, 12.5, 0.10, 500),
    GOLD(1000, Double.MAX_VALUE, 15.0, 0.15, 1000);

    private final double minPoints;
    private final double maxPoints;
    private final double pointsPerHundred;
    private final double maxRedemptionPercentage;
    private final double maxRedeemablePoints;

    public static UserLevel getLevelByPoints(double points) {
        for (UserLevel level : values()) {
            if (points >= level.minPoints && points <= level.maxPoints) {
                return level;
            }
        }
        return BRONZE;
    }
}
