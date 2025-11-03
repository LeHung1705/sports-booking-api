package com.example.booking_api.dto.voucher;

import java.util.UUID;

public class RedeemResponse {
    private UUID redemptionId;
    private String code;
    private double discountValue;

    public RedeemResponse() {}

    public RedeemResponse(UUID redemptionId, String code, double discountValue) {
        this.redemptionId = redemptionId;
        this.code = code;
        this.discountValue = discountValue;
    }

    public UUID getRedemptionId() { return redemptionId; }
    public void setRedemptionId(UUID redemptionId) { this.redemptionId = redemptionId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }
}
