package com.example.booking_api.dto.court;

import com.example.booking_api.entity.enums.SportType;
import lombok.Data;

import java.util.UUID;

@Data
public class CourtResponse {
    private UUID id;
    private UUID venueId;
    private String name;
    private SportType sport;
    private Double pricePerHour;
    private Boolean isActive;
}