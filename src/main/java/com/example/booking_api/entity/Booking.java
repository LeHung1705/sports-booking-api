package com.example.booking_api.entity;

import com.example.booking_api.entity.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "bookings",
        indexes = {
                @Index(columnList = "court_id, start_time, end_time, status", unique = true),
                @Index(columnList = "user_id, start_time")
        }
)
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

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal depositAmount;

    @Column(precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private BookingStatus status = BookingStatus.PENDING;

    private String cancelReason;

    private String refundBankName;
    private String refundAccountNumber;
    private String refundAccountName;
    
    @Column(precision = 15, scale = 2)
    private BigDecimal refundAmount;

    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY)
    private Payment payment;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
}
