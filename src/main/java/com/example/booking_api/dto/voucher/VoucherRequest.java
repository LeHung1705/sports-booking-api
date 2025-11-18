package com.example.booking_api.dto.voucher;

import com.example.booking_api.entity.enums.VoucherType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class VoucherRequest {
    private String code;
    private VoucherType type;           // FIXED or PERCENT
    private BigDecimal value;               // số tiền hoặc %
    private BigDecimal minOrderAmount;      // tối thiểu để được dùng
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;
    private Integer usageLimit;         // tổng số lượt tối đa (nullable = không giới hạn)
    private Boolean active;

}
