package com.example.booking_api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class VenueDetailResponse {
    private UUID id;
    private String name;
    private String address;
    private List<CourtItem> courts;
    private List<ReviewItem> reviews;

    @Data
    @Builder
    public static class CourtItem {
        private UUID id;
        private String name;
        private String sport;
    }

    @Data
    @Builder
    public static class ReviewItem {
        private UUID id;
        private Short rating;
        private String comment;
        private String userName;
        private String courtName;
        private String createdAt;
    }
}