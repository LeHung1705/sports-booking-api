// src/main/java/com/example/booking_api/entity/Voucher.java
package com.example.booking_api.entity;

import com.example.booking_api.entity.enums.VoucherType;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vouchers")
@Data
public class Voucher {

    @Id
    @GeneratedValue
    private UUID id;

    // 👇 NEW: owner của voucher (chủ sân)
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "owner_id", nullable = true)      // BINARY(16)
    private User owner;

    @Column(nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VoucherType type = VoucherType.FIXED;

    // Giá trị giảm
    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal value = BigDecimal.ZERO;

    @Column(precision = 15, scale = 2, nullable = false)
    private BigDecimal minOrderAmount = BigDecimal.ZERO;

    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;

    private Integer usageLimit;

    @Column(nullable = false)
    private Integer usedCount = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
