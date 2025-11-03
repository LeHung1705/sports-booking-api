package com.example.booking_api.dto.voucher;

import com.example.booking_api.entity.enums.VoucherType;

import java.time.OffsetDateTime;

public class VoucherRequest {
    private String code;
    private VoucherType type;           // FIXED or PERCENT
    private Double value;               // số tiền hoặc %
    private Double minOrderAmount;      // tối thiểu để được dùng
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;
    private Integer usageLimit;         // tổng số lượt tối đa (nullable = không giới hạn)
    private Boolean active;

    // getters/setters
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

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
