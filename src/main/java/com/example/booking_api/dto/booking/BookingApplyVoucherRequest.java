package com.example.booking_api.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingApplyVoucherRequest {

    @JsonProperty("voucher_code")
    @NotBlank(message = "voucher_code không được để trống")
    private String voucherCode;
}
