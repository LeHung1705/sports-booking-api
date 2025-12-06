package com.example.booking_api.dto.venue;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class VenueAvailabilityResponse {
    private UUID venueId;
    private String venueName;
    private List<CourtAvailability> courts;

    @Data
    @Builder
    public static class CourtAvailability {
        private UUID courtId;
        private String courtName;
        private List<TimeSlot> slots;
    }

    @Data
    @Builder
    public static class TimeSlot {
        private LocalTime time;
        private LocalTime endTime;
        private BigDecimal price;
        private String status; // available, booked
    }
}
