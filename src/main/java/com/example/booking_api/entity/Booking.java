package com.example.booking_api.entity;

import com.example.booking_api.entity.enums.BookingStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Entity
@Table(name = "bookings", indexes = {
        @Index(columnList = "court_id, start_time, end_time", unique = true),
        @Index(columnList = "user_id, start_time")
})
@Data
public class Booking {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "court_id", nullable = false)
    private Court court;

    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private Double totalAmount;
    private Double discountAmount = 0.0;

    @Enumerated(EnumType.STRING)
    private BookingStatus status = BookingStatus.PENDING;

    private String cancelReason;

    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Getters & Setters
}
