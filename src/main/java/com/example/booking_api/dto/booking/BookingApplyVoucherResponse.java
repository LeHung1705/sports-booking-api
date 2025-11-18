package com.example.booking_api.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class BookingApplyVoucherResponse {

    private UUID id;

    @JsonProperty("original_price")
    private BigDecimal originalPrice;

    @JsonProperty("discount_value")
    private BigDecimal discountValue;

    @JsonProperty("total_amount")
    private BigDecimal totalAmount;

    @JsonProperty("voucher_code")
    private String voucherCode;
}
