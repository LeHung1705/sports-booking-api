package com.example.booking_api.service;

import com.example.booking_api.dto.venue.*;
import com.example.booking_api.entity.*;
import com.example.booking_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VenueService {
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final CourtRepository courtRepository;   // Từ nhánh main
    private final BookingRepository bookingRepository; // Từ nhánh test-feat

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
                .bankBin(req.getBankBin())
                .bankName(req.getBankName())
                .bankAccountNumber(req.getBankAccountNumber())
                .bankAccountName(req.getBankAccountName())
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
                .bankBin(saved.getBankBin())
                .bankName(saved.getBankName())
                .bankAccountNumber(saved.getBankAccountNumber())
                .bankAccountName(saved.getBankAccountName())
                .build();
    }
    // 👇 [BỔ SUNG HÀM NÀY]
    public List<VenueResponse> getMyVenues(String firebaseUid) {
        List<Venue> venues = venueRepository.findByOwner_FirebaseUid(firebaseUid);
        return venues.stream().map(this::mapToVenueResponse).toList();
    }

    // 👇 [BỔ SUNG HÀM NÀY NẾU CHƯA CÓ]
    private VenueResponse mapToVenueResponse(Venue venue) {
        return VenueResponse.builder()
                .id(venue.getId())
                .ownerId(venue.getOwner().getId())
                .name(venue.getName())
                .address(venue.getAddress())
                .district(venue.getDistrict())
                .city(venue.getCity())
                .lat(venue.getLatitude())
                .lng(venue.getLongitude())
                .phone(venue.getPhone())
                .description(venue.getDescription())
                .imageUrl(venue.getImageUrl())
                .isActive(venue.getIsActive())
                .bankBin(venue.getBankBin())
                .bankName(venue.getBankName())
                .bankAccountNumber(venue.getBankAccountNumber())
                .bankAccountName(venue.getBankAccountName())
                .build();
    }
    // Sử dụng logic từ MAIN (Có tính năng aggregation giá min/max)
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

            if (rawIds.isEmpty()) return List.of();

            List<UUID> ids = rawIds.stream()
                    .map(bytes -> {
                        ByteBuffer bb = ByteBuffer.wrap(bytes);
                        long high = bb.getLong();
                        long low = bb.getLong();
                        return new UUID(high, low);
                    })
                    .toList();

            List<Venue> venues = venueRepository.findByIdIn(ids);
            if (venues.isEmpty()) return List.of();

            List<UUID> venueIds = venues.stream().map(Venue::getId).toList();

            // Logic từ Main: Map giá min/max
            Map<UUID, CourtRepository.VenuePriceAgg> priceAggMap = courtRepository
                    .getPriceAggByVenueIds(venueIds)
                    .stream()
                    .collect(Collectors.toMap(
                            CourtRepository.VenuePriceAgg::getVenueId,
                            Function.identity()
                    ));

            return venues.stream().map(v -> {
                ReviewRepository.ReviewStats stats = reviewRepository.getVenueStats(v.getId());
                Double avgRating = null;
                if (stats != null && stats.getAvg() != null) {
                    avgRating = Math.round(stats.getAvg() * 10.0) / 10.0;
                }

                CourtRepository.VenuePriceAgg agg = priceAggMap.get(v.getId());

                return VenueListResponse.builder()
                        .id(v.getId())
                        .name(v.getName())
                        .address(v.getAddress())
                        .district(v.getDistrict())
                        .city(v.getCity())
                        .phone(v.getPhone())
                        .imageUrl(v.getImageUrl())
                        .avgRating(avgRating)
                        .minPrice(agg == null ? null : agg.getMinPrice())
                        .maxPrice(agg == null ? null : agg.getMaxPrice())
                        .lat(v.getLatitude())
                        .lng(v.getLongitude())
                        .build();

            }).toList();

        } catch (Exception e) {
            throw new RuntimeException("QUERY_ERROR", e);
        }
    }

    // Sử dụng logic từ MAIN (Có reviewCount và chi tiết Court update hơn)
    public VenueDetailResponse getVenueDetail(UUID id) {
        Venue venue = venueRepository.findWithCourtsById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sân"));

        ReviewRepository.ReviewStats stats = reviewRepository.getVenueStats(id);
        Double avgRating = null;
        Long reviewCount = null;

        if (stats != null) {
            if (stats.getAvg() != null) {
                avgRating = Math.round(stats.getAvg() * 10.0) / 10.0;
            }
            reviewCount = stats.getCount();
        }

        // Top 3 reviews (Optional: Nếu bạn muốn giữ cả list review từ test-feat, có thể uncomment đoạn dưới)
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
                .reviewCount(reviewCount)
                .courts(venue.getCourts() == null ? List.of()
                        : venue.getCourts().stream()
                        .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                        .map(c -> VenueDetailResponse.CourtItem.builder()
                                .id(c.getId())
                                .name(c.getName())
                                .sport(c.getSport() == null ? null : c.getSport().name())
                                .imageUrl(c.getImageUrl())
                                .pricePerHour(c.getPricePerHour())
                                .build())
                        .toList())
                // Merge thêm phần reviews từ test-feat vào cấu trúc của main (nếu DTO hỗ trợ)
                .reviews(top3.stream()
                        .map(r -> VenueDetailResponse.ReviewItem.builder()
                                .id(r.getId())
                                .rating(r.getRating() == null ? null : r.getRating().doubleValue())
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

        if (req.getName() != null) venue.setName(req.getName());
        if (req.getAddress() != null) venue.setAddress(req.getAddress());
        if (req.getDistrict() != null) venue.setDistrict(req.getDistrict());
        if (req.getCity() != null) venue.setCity(req.getCity());
        if (req.getPhone() != null) venue.setPhone(req.getPhone());
        if (req.getDescription() != null) venue.setDescription(req.getDescription());
        if (req.getLat() != null) venue.setLatitude(req.getLat());
        if (req.getLng() != null) venue.setLongitude(req.getLng());
        if (req.getImageUrl() != null) venue.setImageUrl(req.getImageUrl());
        if (req.getBankBin() != null) venue.setBankBin(req.getBankBin());
        if (req.getBankName() != null) venue.setBankName(req.getBankName());
        if (req.getBankAccountNumber() != null) venue.setBankAccountNumber(req.getBankAccountNumber());
        if (req.getBankAccountName() != null) venue.setBankAccountName(req.getBankAccountName());

        // Venue uses OffsetDateTime for now, assuming not refactored yet.
        // If Venue was refactored, this should be LocalDateTime.now()
        // I will stick to OffsetDateTime for Venue as instructed "Booking entity" was the focus.
        // But wait, the user said "VenueService.java has same error, fix it".
        // This implies Venue might also be using LocalDateTime or interacting with Booking.
        // In getVenueAvailability, it interacts with Booking.
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
                .bankBin(saved.getBankBin())
                .bankName(saved.getBankName())
                .bankAccountNumber(saved.getBankAccountNumber())
                .bankAccountName(saved.getBankAccountName())
                .build();
    }

    public void deleteVenue(String firebaseUid, UUID venueId) {
        User user = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Venue venue = venueRepository.findById(venueId).orElseThrow(() -> new RuntimeException("Venue not found"));

        String role = user.getRole() == null ? "" : user.getRole().toString();

        boolean isOwner = venue.getOwner() != null && venue.getOwner().getId().equals(user.getId());
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);

        if (!isOwner && !isAdmin) {
            throw new SecurityException("Not allowed");
        }

        venueRepository.delete(venue);
    }

    // --- NEW METHOD FROM TEST-FEAT: Availability ---
    public VenueAvailabilityResponse getVenueAvailability(UUID venueId, LocalDate date) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new RuntimeException("Venue not found"));

        System.out.println("=== VENUE AVAILABILITY DEBUG ===");
        System.out.println("Venue ID: " + venueId);
        System.out.println("Date: " + date);

        // 1. Get all bookings for this venue on this date
        // REFACTOR: Use LocalDateTime directly as BookingRepository now expects LocalDateTime
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();
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
        LocalTime openTime = LocalTime.of(5, 0);  // 05:00
        LocalTime closeTime = LocalTime.of(23, 0); // 23:00

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");

        List<VenueAvailabilityResponse.CourtAvailability> courtAvailabilities = courts.stream().map(court -> {
            List<VenueAvailabilityResponse.TimeSlot> slots = new ArrayList<>();
            LocalTime current = openTime;

            while (current.isBefore(closeTime)) {
                LocalTime next = current.plusMinutes(30);
                // REFACTOR: Use LocalDateTime directly
                LocalDateTime slotStart = LocalDateTime.of(date, current);
                LocalDateTime slotEnd = LocalDateTime.of(date, next);

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
