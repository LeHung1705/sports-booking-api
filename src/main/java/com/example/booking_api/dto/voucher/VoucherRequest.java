package com.example.booking_api.dto.voucher;

import com.example.booking_api.entity.enums.VoucherType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List; // 👈 ĐÃ BỔ SUNG IMPORT NÀY
// 👇👇👇 QUAN TRỌNG: Hãy chắc chắn bạn Import dòng này
import java.util.UUID;
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
    // 👇 THÊM MỚI: Danh sách ID các sân được chọn
    private List<UUID> venueIds;
}
