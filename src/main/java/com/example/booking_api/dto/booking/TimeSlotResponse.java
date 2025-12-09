package com.example.booking_api.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class TimeSlotResponse {
    private String time;       // Changed from LocalTime to String
    private String endTime;    // Changed from LocalTime to String
    private BigDecimal price;
    private String status;     // "available" or "booked"
}