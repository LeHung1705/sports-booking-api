package com.example.booking_api.entity;

import com.example.booking_api.entity.enums.VoucherType;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "vouchers", uniqueConstraints = {
        @UniqueConstraint(columnNames = "code")
})
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
    @Column(nullable = false)
    private Double value = 0.0;

    // Đơn tối thiểu để được áp dụng
    private Double minOrderAmount = 0.0;

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

    /* ----------------- GETTER/SETTER thủ công (tránh phụ thuộc Lombok) ----------------- */

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public VoucherType getType() { return type; }
    public void setType(VoucherType type) { this.type = type; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public Double getMinOrderAmount() { return minOrderAmount; }
    public void setMinOrderAmount(Double minOrderAmount) { this.minOrderAmount = minOrderAmount; }

    public OffsetDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(OffsetDateTime validFrom) { this.validFrom = validFrom; }

    public OffsetDateTime getValidTo() { return validTo; }
    public void setValidTo(OffsetDateTime validTo) { this.validTo = validTo; }

    public Integer getUsageLimit() { return usageLimit; }
    public void setUsageLimit(Integer usageLimit) { this.usageLimit = usageLimit; }

    public Integer getUsedCount() { return usedCount; }
    public void setUsedCount(Integer usedCount) { this.usedCount = usedCount; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
