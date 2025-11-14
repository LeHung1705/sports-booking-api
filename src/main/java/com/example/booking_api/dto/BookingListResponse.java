package com.example.booking_api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class BookingListResponse {
    private UUID id;
    private String court;
    private OffsetDateTime startTime;
    private String status;
}
