package com.example.booking_api.dto.payment;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PaymentCreateResponse {
    private String paymentUrl;
    private UUID paymentId;
}
