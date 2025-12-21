package com.example.booking_api.entity.enums;

public enum NotificationType {
    BOOKING_CONFIRMED,
    PAYMENT_SUCCESS,
    REMINDER,
    SYSTEM,
    BOOKING_CREATED,// 👈 THÊM DÒNG NÀY (Để báo cho Owner có khách mới)
    BOOKING_CANCELLED
}
