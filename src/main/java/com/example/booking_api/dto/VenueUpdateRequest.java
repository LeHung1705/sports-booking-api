package com.example.booking_api.dto;

import lombok.Data;

@Data
public class VenueUpdateRequest {
    private String name;
    private String address;
    private Boolean isActive;
}