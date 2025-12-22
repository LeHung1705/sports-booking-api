package com.example.booking_api.entity;
import jakarta.persistence.*;
import com.example.booking_api.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
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
    // 👇 BỔ SUNG THÊM DÒNG NÀY (Để liên kết với đơn hàng)
    @Column(name = "booking_id", columnDefinition = "BINARY(16)")
    private UUID bookingId;

    @Column(name = "venue_id", columnDefinition = "BINARY(16)")
    private UUID venueId;

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

    // 👇 Cột thời gian
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
    // 👇 THÊM ĐOẠN NÀY VÀO: Tự động lưu thời gian khi Insert
    // 👇 ĐOẠN CODE QUAN TRỌNG ĐỂ TỰ ĐỘNG LƯU GIỜ
    // 👇 HÀM TỰ ĐỘNG LƯU THỜI GIAN (Fix lỗi NULL)
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            // Dùng OffsetDateTime.now() để khớp với kiểu dữ liệu
            this.createdAt = OffsetDateTime.now();
        }
    }
}