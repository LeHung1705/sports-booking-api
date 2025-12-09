package com.example.booking_api.dto.review;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class CourtReviewResponse {
    private UUID id;
    private Short rating;
    private String comment;
    private String userName;
    private OffsetDateTime createdAt;
}
