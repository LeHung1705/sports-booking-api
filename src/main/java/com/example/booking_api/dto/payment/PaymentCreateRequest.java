package com.example.booking_api.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentCreateRequest {
    @JsonProperty("booking_id")
    @NotNull(message = "booking_id không được để trống")
    private UUID bookingId;
}
