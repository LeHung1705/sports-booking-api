package com.example.booking_api.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class PaymentCreateResponse {
    private String paymentUrl;
    private UUID paymentId;
}
