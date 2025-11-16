package com.example.booking_api.dto.voucher;

import java.util.UUID;

public class RedeemRequest {
    private UUID userId;
    private UUID bookingId;
    private String code;
    private Double discountValue; // số tiền đã áp khi thanh toán thành công

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public Double getDiscountValue() { return discountValue; }
    public void setDiscountValue(Double discountValue) { this.discountValue = discountValue; }
}
