package com.example.booking_api.entity;

import com.example.booking_api.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;

    // user_id trong DB là BINARY(16) -> map sang UUID
    @Column(name = "user_id", nullable = false, columnDefinition = "BINARY(16)")
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "body", nullable = false, length = 255)
    private String body;

    // cột `read` là bit(1)
    @Column(name = "`read`", nullable = false)
    private boolean read;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}