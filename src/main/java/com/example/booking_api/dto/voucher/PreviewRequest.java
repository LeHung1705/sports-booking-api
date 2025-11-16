package com.example.booking_api.dto.voucher;

import java.util.UUID;

public class PreviewRequest {
    private String code;
    private Double orderAmount;   // tổng tiền hiện tại của booking (chưa trừ)
    private UUID userId;          // tùy dùng/không

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Double getOrderAmount() { return orderAmount; }
    public void setOrderAmount(Double orderAmount) { this.orderAmount = orderAmount; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
}
