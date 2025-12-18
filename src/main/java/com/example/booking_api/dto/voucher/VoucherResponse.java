package com.example.booking_api.dto.voucher;

import com.example.booking_api.entity.enums.VoucherType;
import lombok.AllArgsConstructor; // Thêm
import lombok.Builder;          // Quan trọng: Thêm dòng này
import lombok.Data;
import lombok.NoArgsConstructor; // Thêm

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder // 👈 BẮT BUỘC PHẢI CÓ ĐỂ SỬ DỤNG .builder()
@NoArgsConstructor
@AllArgsConstructor
public class VoucherResponse {
    private UUID id;
    private String code;
    private VoucherType type;
    private BigDecimal value;
    private BigDecimal minOrderAmount;
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;
    private Integer usageLimit;
    private Integer usedCount;
    private Boolean active;
}