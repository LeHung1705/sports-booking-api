package com.example.booking_api.dto.booking;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@Builder
public class TimeSlotResponse {
    private LocalTime time;
    private LocalTime endTime;
    private BigDecimal price;
    private String status; // AVAILABLE, BOOKED
}
