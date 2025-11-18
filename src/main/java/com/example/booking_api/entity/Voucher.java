package com.example.booking_api.entity;

import com.example.booking_api.entity.enums.VoucherType;
import jakarta.persistence.*;

import java.math.BigDecimal;
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

    @Column(nullable = false, length = 64)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VoucherType type = VoucherType.FIXED;

    // Giá trị giảm: nếu type = PERCENT -> 0..100 ; nếu type = FIXED -> số tiền
    @Column(precision = 15, scale = 2)
    private BigDecimal value = BigDecimal.valueOf(0);

    @Column(precision = 15, scale = 2)
    private BigDecimal minOrderAmount = BigDecimal.valueOf(0);

    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;

    // Giới hạn tổng số lần dùng (toàn hệ thống). null = không giới hạn
    private Integer usageLimit;

    // Đã dùng bao nhiêu lần (toàn hệ thống)
    @Column(nullable = false)
    private Integer usedCount = 0;

    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // Getters & Setters
}

