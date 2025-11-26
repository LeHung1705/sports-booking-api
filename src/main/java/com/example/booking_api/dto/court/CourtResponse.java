package com.example.booking_api.dto.court;

import com.example.booking_api.entity.enums.SportType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class CourtResponse {
    private UUID id;
    private UUID venueId;
    private String name;
    private SportType sport;
    private BigDecimal pricePerHour;
    private String imageUrl;
    private Boolean isActive;
}