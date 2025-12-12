package com.example.booking_api.entity.enums;

public enum BookingStatus {
    PENDING,
    PENDING_PAYMENT,
    AWAITING_CONFIRM,
    CONFIRMED,
    REFUND_PENDING,
    CANCELED,
    REJECTED,
    COMPLETED,
    FAILED
}
