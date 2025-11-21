// src/main/java/com/example/booking_api/dto/voucher/PreviewRequest.java
package com.example.booking_api.dto.voucher;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PreviewRequest {
    private String code;
    private BigDecimal orderAmount;
    private UUID userId;

    // 👇 NEW: venue để đối chiếu owner
    private UUID venueId;
}
