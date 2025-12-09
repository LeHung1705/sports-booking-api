package com.example.booking_api.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BookingDetailResponse {
    private UUID id;
    private String court;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private PaymentItem payment;

    @Builder
    @Data
    public static class PaymentItem {
        private UUID id;
        private BigDecimal amount;
        private String status;
        private String returnPayload;
    }
}
