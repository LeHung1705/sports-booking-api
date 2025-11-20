package com.example.booking_api.dto.venue;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class VenueListResponse {
    private UUID id;
    private String name;
    private String address;
    private String imageUrl;
    private List<CourtItem> courts;

    @Data
    @Builder
    public static class CourtItem {
        private UUID id;
        private String name;
        private String sport;
    }
}
