package com.example.booking_api.dto.review;

import java.time.OffsetDateTime;
import java.util.UUID;

public class ReviewItemResponse {
    private UUID id;
    private Integer rating;
    private String comment;
    private String userFullName;
    private OffsetDateTime createdAt;

    public ReviewItemResponse() {}

    public ReviewItemResponse(UUID id, Integer rating, String comment, String userFullName, OffsetDateTime createdAt) {
        this.id = id;
        this.rating = rating;
        this.comment = comment;
        this.userFullName = userFullName;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getUserFullName() { return userFullName; }
    public void setUserFullName(String userFullName) { this.userFullName = userFullName; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
