package com.example.booking_api.entity;

import com.example.booking_api.entity.enums.NotificationType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
        @Index(columnList = "user_id, read")
})
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    private String title;
    private String body;
    private Boolean read = false;
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // Getters & Setters
}
