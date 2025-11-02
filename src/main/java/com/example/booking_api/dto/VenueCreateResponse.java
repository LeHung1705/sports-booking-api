package com.example.booking_api.dto;

import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class VenueCreateResponse {
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
}
