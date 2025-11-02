package com.example.booking_api.entity;

import com.example.booking_api.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Entity
@Table(name = "payments")
@Data
public class Payment {

    @Id
    @GeneratedValue
    private UUID id;

    @OneToOne
    @JoinColumn(name = "booking_id", nullable = false, unique = true)
    private Booking booking;

    private String provider = "VNPAY";

    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.INIT;

    private Double amount;
    private String providerTxnRef;
    private OffsetDateTime paidAt;

    @Column(name = "return_payload", columnDefinition = "json")
    private String returnPayload;

    private OffsetDateTime createdAt = OffsetDateTime.now();
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Getters & Setters
}
