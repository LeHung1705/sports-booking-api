package com.example.booking_api.dto.booking;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
public class BookingCreateRequest {

    @NotNull(message = "court_id is required")
    private UUID courtId;

    @NotNull(message = "start_time is required")
    @Future(message = "start_time must be in the future")
    private OffsetDateTime startTime;

    @NotNull(message = "end_time is required")
    @Future(message = "end_time must be in the future")
    private OffsetDateTime endTime;

    // Custom validator trong DTO
    @AssertTrue(message = "start_time must be before end_time")
    public boolean isValidTimeRange() {
        if (startTime == null || endTime == null) return true;
        return startTime.isBefore(endTime);
    }
}
