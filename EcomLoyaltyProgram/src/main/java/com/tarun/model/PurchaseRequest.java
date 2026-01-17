package com.tarun.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class PurchaseRequest {
    public final String userName;
    public  final double orderAmount;
    public  final double pointsToRedeem;
}
