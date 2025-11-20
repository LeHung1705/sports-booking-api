package com.example.booking_api.dto;

import com.example.booking_api.entity.Notification;
import com.example.booking_api.entity.enums.NotificationType;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private String title;
    private String body;
    private OffsetDateTime createdAt;
    private boolean read;
    private NotificationType type;

    public static NotificationResponse fromEntity(Notification n) {
        if (n == null) return null;

        return NotificationResponse.builder()
                .id(n.getId())
                .title(n.getTitle())
                .body(n.getBody())
                .createdAt(n.getCreatedAt())
                .read(n.isRead())
                .type(n.getType())
                .build();
    }
}
