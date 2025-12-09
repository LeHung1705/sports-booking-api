package com.example.booking_api.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class BookingListResponse {
    private UUID id;
    private String court;
    private LocalDateTime startTime;
    private String status;
}
