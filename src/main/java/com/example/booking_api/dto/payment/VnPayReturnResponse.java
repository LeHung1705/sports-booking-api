package com.example.booking_api.dto.payment;

import com.example.booking_api.entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class VnPayReturnResponse {
    private UUID bookingId;
    private UUID paymentId;

    private PaymentStatus paymentStatus;
    private String vnpResponseCode;
    private String vnpTransactionStatus;

    private String message;
}
