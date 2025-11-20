package com.example.booking_api.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Entity
@Table(name = "voucher_redemptions", indexes = {
        @Index(columnList = "voucher_id, booking_id", unique = true)
})
@Data
public class VoucherRedemption {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(precision = 15, scale = 2)
    private BigDecimal discountValue;
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // Getters & Setters
}
