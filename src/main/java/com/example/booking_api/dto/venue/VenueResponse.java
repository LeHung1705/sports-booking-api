package com.example.booking_api.dto.venue;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class VenueResponse {
    private UUID id;
    private UUID ownerId;
    private String name;
    private String address;
    private String district;
    private String city;
    private Double lat;
    private Double lng;
    private String phone;
    private String description;
    private String imageUrl;
    private Boolean isActive;
    
    private String bankBin;
    private String bankName;
    private String bankAccountNumber;
    private String bankAccountName;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "HH:mm")
    private java.time.LocalTime openTime;
    @com.fasterxml.jackson.annotation.JsonFormat(shape = com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING, pattern = "HH:mm")
    private java.time.LocalTime closeTime;
}
