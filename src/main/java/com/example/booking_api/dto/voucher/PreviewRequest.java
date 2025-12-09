// src/main/java/com/example/booking_api/dto/voucher/PreviewRequest.java
package com.example.booking_api.dto.voucher;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class PreviewRequest {
    @JsonProperty("code")
    private String code;
    
    @JsonProperty("order_amount")
    private BigDecimal orderAmount;
    
    @JsonProperty("user_id")
    private UUID userId;

    // 👇 NEW: venue để đối chiếu owner
    @JsonProperty("venue_id")
    private UUID venueId;
}
