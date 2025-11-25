package com.example.booking_api.dto.venue;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;

@Data
@Builder
public class VenueDetailResponse {
    private UUID id;
    private String name;
    private String address;
    private String district;
    private String city;
    private String phone;
    private String description;
    private String imageUrl;

    private Double avgRating;
    private Long reviewCount;

    private List<CourtItem> courts;

    @Data
    @Builder
    public static class CourtItem {
        private UUID id;
        private String name;
        private String sport;
        private String imageUrl;
        private BigDecimal pricePerHour;
    }
}
