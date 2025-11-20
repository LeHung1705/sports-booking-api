package com.example.booking_api.dto.voucher;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RedeemRequest {
    private UUID userId;
    private UUID bookingId;
    private String code;
    private BigDecimal discountValue;

}
