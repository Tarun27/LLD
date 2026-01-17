package com.tarun.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PurchaseResult {

    private final boolean success;
    private final String message;
    private final double pointsRedeemed;
    private final double pointsAdded;
    private final double discountApplied;
    private final double totalPayable;
    private final double currentPoints;
    private final UserLevel currentLevel;

}
