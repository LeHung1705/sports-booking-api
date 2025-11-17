package com.example.booking_api.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class PaymentCreateRequest {

    @NotNull(message = "booking_id không được để trống")
    private UUID bookingId;
}
