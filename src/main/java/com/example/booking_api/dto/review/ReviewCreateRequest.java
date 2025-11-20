package com.example.booking_api.dto.review;

import java.util.UUID;

public class ReviewCreateRequest {
    private UUID bookingId;
    private Integer rating;   // 1..5
    private String comment;

    public UUID getBookingId() { return bookingId; }
    public void setBookingId(UUID bookingId) { this.bookingId = bookingId; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
