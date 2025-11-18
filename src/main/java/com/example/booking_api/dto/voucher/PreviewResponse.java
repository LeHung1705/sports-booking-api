package com.example.booking_api.dto.voucher;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PreviewResponse {
    private boolean valid;
    private BigDecimal discount;
    private String reason;


    public PreviewResponse(boolean valid, BigDecimal discount, String reason) {
        this.valid = valid;
        this.discount = discount;
        this.reason = reason;
    }
}
