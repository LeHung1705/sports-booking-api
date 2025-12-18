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
    private String venue;
    private String court;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createdAt;
    private BigDecimal totalPrice;
    private BigDecimal discountAmount;
    private String voucherCode;
    private String status;
    private PaymentItem payment;

    // Venue Bank Info
    private String bankBin;
    private String bankAccountNumber;
    private String bankAccountName;

    // Refund Info
    private BigDecimal refundAmount;
    private String refundBankName;
    private String refundAccountNumber;
    private String refundAccountName;

    @Builder
    @Data
    public static class PaymentItem {
        private UUID id;
        private BigDecimal amount;
        private String status;
        private String returnPayload;
    }
}
