package com.example.booking_api.dto.voucher;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RedeemResponse {
    private UUID redemptionId;
    private String code;
    private BigDecimal discountValue;


    public RedeemResponse(UUID redemptionId, String code, BigDecimal discountValue) {
        this.redemptionId = redemptionId;
        this.code = code;
        this.discountValue = discountValue;
    }
}
