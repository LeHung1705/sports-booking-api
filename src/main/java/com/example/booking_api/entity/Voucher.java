package com.example.booking_api.entity;

import com.example.booking_api.entity.enums.VoucherType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Data;

@Entity
@Table(name = "vouchers")
@Data
public class Voucher {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(unique = true, nullable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    private VoucherType type;

    private Double value;
    private Double minOrderAmount;
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;
    private Boolean active = true;
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // Getters & Setters
}
