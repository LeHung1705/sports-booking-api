package com.example.booking_api.service;

import com.example.booking_api.dto.venue.*;
import com.example.booking_api.entity.*;

import com.example.booking_api.repository.UserRepository;
import com.example.booking_api.repository.ReviewRepository;
import com.example.booking_api.repository.BookingRepository;
import com.example.booking_api.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VenueService {
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;

    public VenueResponse createVenue(String firebaseUid, VenueCreateRequest req) {
        User owner = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String role = owner.getRole() == null ? "" : owner.getRole().toString();
        if (!"OWNER".equalsIgnoreCase(role)) {
            throw new SecurityException("Not allowed");
        }

        Venue v = Venue.builder()
                .owner(owner)
                .name(req.getName())
                .address(req.getAddress())
                .district(req.getDistrict())
                .city(req.getCity())
                .latitude(req.getLat())
                .longitude(req.getLng())
                .phone(req.getPhone())
                .description(req.getDescription())
                .imageUrl(req.getImageUrl())
                .isActive(true)
                .build();
        Venue saved = venueRepository.save(v);

        return VenueResponse.builder()
                .id(saved.getId())
                .ownerId(saved.getOwner().getId())
                .name(saved.getName())
                .address(saved.getAddress())
                .district(saved.getDistrict())
                .city(saved.getCity())
                .lat(saved.getLatitude())
                .lng(saved.getLongitude())
                .phone(saved.getPhone())
                .description(saved.getDescription())
                .imageUrl(saved.getImageUrl())
                .isActive(saved.getIsActive())
                .build();
    }

    public List<VenueListResponse> searchVenues(VenueListRequest req) {
        try {
            List<byte[]> rawIds = venueRepository.findIds(
                    nullIfBlank(req.getQ()),
                    nullIfBlank(req.getCity()),
                    nullIfBlank(req.getSport()),
                    req.getLat(),
                    req.getLng(),
                    req.getRadius()
            );

            if (rawIds.isEmpty()) {
                return List.of();
            }

            List<UUID> ids = rawIds.stream()
                    .map(bytes -> {
                        ByteBuffer bb = ByteBuffer.wrap(bytes);
                        long high = bb.getLong();
                        long low = bb.getLong();
                        return new UUID(high, low);
                    })
                    .toList();

            List<Venue> venues = venueRepository.findByIdIn(ids);

            return venues.stream().map(v ->
                    VenueListResponse.builder()
                            .id(v.getId())
                            .name(v.getName())
                            .address(v.getAddress())
                            .imageUrl(v.getImageUrl())
                            .courts(v.getCourts() == null ? List.of()
                                    : v.getCourts().stream()
                                    .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                                    .map(c -> VenueListResponse.CourtItem.builder()
                                            .id(c.getId())
                                            .name(c.getName())
                                            .sport(c.getSport() == null ? null : c.getSport().name())
                                            .build())
                                    .toList())
                            .build()
            ).toList();

        } catch (Exception e) {
            throw new RuntimeException("QUERY_ERROR", e);
        }
    }

    public VenueDetailResponse getVenueDetail(UUID id) {
        Venue venue = venueRepository.findWithCourtsById(id).orElseThrow(() -> new RuntimeException("Không tìm thấy sân"));

        // Get avg venue
        ReviewRepository.ReviewStats stats = reviewRepository.getVenueStats(id);
        Double avgRating = null;
        if (stats != null && stats.getAvg() != null) {
            avgRating = Math.round(stats.getAvg() * 10.0) / 10.0;
        }

        // Top 3 review
        List<Review> top3 = reviewRepository.findTopByVenue(id, PageRequest.of(0, 3));

        return VenueDetailResponse.builder()
                .id(venue.getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .district(venue.getDistrict())
                .city(venue.getCity())
                .phone(venue.getPhone())
                .description(venue.getDescription())
                .imageUrl(venue.getImageUrl())
                .avgRating(avgRating)
                .courts(venue.getCourts() == null ? List.of()
                        : venue.getCourts().stream()
                        .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                        .map(c -> VenueDetailResponse.CourtItem.builder()
                                .id(c.getId())
                                .name(c.getName())
                                .sport(c.getSport() == null ? null : c.getSport().name())
                                .build())
                        .toList())
                .reviews(top3.stream()
                        .map(r -> VenueDetailResponse.ReviewItem.builder()
                                .id(r.getId())
                                .rating(r.getRating())
                                .comment(r.getComment())
                                .userName(r.getUser().getFullName())
                                .courtName(r.getCourt().getName())
                                .createdAt(r.getCreatedAt() == null ? null : r.getCreatedAt().toString())
                                .build())
                        .toList())
                .build();
    }

    public VenueResponse updateVenue(String firebaseUid, UUID venueId, VenueUpdateRequest req) {
        User owner = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new RuntimeException("Venue not found"));
        if (!venue.getOwner().getId().equals(owner.getId())) {
            throw new SecurityException("Not allowed");
        }

        if (req.getName() != null) {
            venue.setName(req.getName());
        }
        if (req.getAddress() != null) {
            venue.setAddress(req.getAddress());
        }
        if (req.getDistrict() != null) {
            venue.setDistrict(req.getDistrict());
        }
        if (req.getCity() != null) {
            venue.setCity(req.getCity());
        }
        if (req.getPhone() != null) {
            venue.setPhone(req.getPhone());
        }
        if (req.getDescription() != null) {
            venue.setDescription(req.getDescription());
        }
        if (req.getLat() != null) {
            venue.setLatitude(req.getLat());
        }
        if (req.getLng() != null) {
            venue.setLongitude(req.getLng());
        }
        if (req.getImageUrl() != null) {
            venue.setImageUrl(req.getImageUrl());
        }

        venue.setUpdatedAt(OffsetDateTime.now());
        Venue saved = venueRepository.save(venue);

        return VenueResponse.builder()
                .id(saved.getId())
                .ownerId(saved.getOwner().getId())
                .name(saved.getName())
                .address(saved.getAddress())
                .district(saved.getDistrict())
                .city(saved.getCity())
                .lat(saved.getLatitude())
                .lng(saved.getLongitude())
                .phone(saved.getPhone())
                .description(saved.getDescription())
                .imageUrl(saved.getImageUrl())
                .isActive(saved.getIsActive())
                .build();
    }

    public void deleteVenue(String firebaseUid, UUID venueId) {
        User user = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Venue venue = venueRepository.findById(venueId).orElseThrow(() -> new RuntimeException("Venue not found"));

        String role = user.getRole() == null ? "" : user.getRole().toString();

        boolean isOwner = venue.getOwner() != null
                && venue.getOwner().getId().equals(user.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);

        if (!isOwner && !isAdmin) {
            throw new SecurityException("Not allowed");
        }

        venueRepository.delete(venue);
    }

    public VenueAvailabilityResponse getVenueAvailability(UUID venueId, LocalDate date) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new RuntimeException("Venue not found"));

        System.out.println("=== VENUE AVAILABILITY DEBUG ===");
        System.out.println("Venue ID: " + venueId);
        System.out.println("Date: " + date);

        // 1. Get all bookings for this venue on this date
        OffsetDateTime startOfDay = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        List<Booking> bookings = bookingRepository.findByVenueAndDateRange(venueId, startOfDay, endOfDay);

        System.out.println("Found " + bookings.size() + " bookings for venue");
        bookings.forEach(b -> {
            System.out.println(String.format(
                    "  Booking: court=%s, [%s to %s], status=%s",
                    b.getCourt().getName(), b.getStartTime(), b.getEndTime(), b.getStatus()
            ));
        });

        // 2. Get all active courts
        List<Court> courts = venue.getCourts().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .toList();

        // 3. Generate 30-minute slots
        LocalTime openTime = LocalTime.of(5, 0);  // Changed to 05:00
        LocalTime closeTime = LocalTime.of(23, 0); // Changed to 23:00

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

        List<VenueAvailabilityResponse.CourtAvailability> courtAvailabilities = courts.stream().map(court -> {
            List<VenueAvailabilityResponse.TimeSlot> slots = new ArrayList<>();
            LocalTime current = openTime;

            while (current.isBefore(closeTime)) {
                LocalTime next = current.plusMinutes(30);
                OffsetDateTime slotStart = date.atTime(current).atOffset(ZoneOffset.UTC);
                OffsetDateTime slotEnd = date.atTime(next).atOffset(ZoneOffset.UTC);

                // Check price for 30-min slot
                BigDecimal pricePerHour = getPriceForSlot(venue, current, next);
                if (pricePerHour == null) pricePerHour = court.getPricePerHour();

                // Calculate 30-min price (half of hourly rate)
                BigDecimal slotPrice = pricePerHour.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP);

                // Check if slot overlaps with any booking
                boolean isBooked = bookings.stream().anyMatch(b ->
                        b.getCourt().getId().equals(court.getId()) &&
                                b.getStartTime().isBefore(slotEnd) &&
                                b.getEndTime().isAfter(slotStart)
                );

                String timeStr = current.format(formatter);
                String endTimeStr = next.format(formatter);

                slots.add(VenueAvailabilityResponse.TimeSlot.builder()
                        .time(timeStr)      // String format "HH:mm"
                        .endTime(endTimeStr) // String format "HH:mm"
                        .price(slotPrice)    // 30-min price
                        .status(isBooked ? "booked" : "available")
                        .build());

                current = next;
            }

            System.out.println(String.format("Court %s: generated %d slots", court.getName(), slots.size()));

            return VenueAvailabilityResponse.CourtAvailability.builder()
                    .courtId(court.getId())
                    .courtName(court.getName())
                    .slots(slots)
                    .build();
        }).toList();

        System.out.println("=== END VENUE AVAILABILITY DEBUG ===");

        return VenueAvailabilityResponse.builder()
                .venueId(venue.getId())
                .venueName(venue.getName())
                .courts(courtAvailabilities)
                .build();
    }

    private BigDecimal getPriceForSlot(Venue venue, LocalTime start, LocalTime end) {
        if (venue.getPricingConfig() == null) return null;

        for (PricingRule rule : venue.getPricingConfig()) {
            // Simple logic: if slot falls within rule range
            // Example: Rule 17:00-22:00. Slot 17:00-18:00 -> Start(17) >= RuleStart(17) AND End(18) <= RuleEnd(22)
            if (!start.isBefore(rule.getStartTime()) && !end.isAfter(rule.getEndTime())) {
                return rule.getPricePerHour();
            }
        }
        return null;
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}