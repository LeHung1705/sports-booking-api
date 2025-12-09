package com.example.booking_api.dto.venue;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class VenueListResponse {
    private UUID id;
    private String name;
    private String address;
    private String district;
    private String city;
    private String phone;
    private String imageUrl;
    private Double avgRating;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Double lat;
    private Double lng;
}
