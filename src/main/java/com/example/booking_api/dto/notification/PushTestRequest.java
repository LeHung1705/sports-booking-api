package com.example.booking_api.dto.notification;

import com.example.booking_api.entity.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PushTestRequest {
    private String title;
    private String body;
    private NotificationType type = NotificationType.SYSTEM;

    // Tuỳ chọn: nếu không truyền, sẽ bắn FCM cho chính người đang đăng nhập
    private String targetFirebaseUid;
}