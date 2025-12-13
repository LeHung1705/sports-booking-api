package com.example.booking_api.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.AssertTrue;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class BookingCreateRequest {
    @JsonProperty("court_id")
    @NotNull(message = "court_id is required")
    private UUID courtId;

    @JsonProperty("start_time")
    @NotNull(message = "start_time is required")
    @Future(message = "start_time must be in the future")
    private LocalDateTime startTime;

    @JsonProperty("end_time")
    @NotNull(message = "end_time is required")
    @Future(message = "end_time must be in the future")
    private LocalDateTime endTime;


    @JsonProperty("payment_option")
    private String paymentOption; // "DEPOSIT" or "FULL_PAYMENT"

    @AssertTrue(message = "start_time must be before end_time")
    public boolean isValidTimeRange() {
        if (startTime == null || endTime == null) return true;
        return startTime.isBefore(endTime);
    }
}
