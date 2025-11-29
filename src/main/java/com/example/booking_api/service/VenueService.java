package com.example.booking_api.service;

import com.example.booking_api.dto.venue.*;
import com.example.booking_api.entity.Review;
import com.example.booking_api.entity.User;
import com.example.booking_api.entity.Venue;
import com.example.booking_api.repository.CourtRepository;
import com.example.booking_api.repository.ReviewRepository;
import com.example.booking_api.repository.UserRepository;
import com.example.booking_api.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VenueService {
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private  final CourtRepository courtRepository;

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

            if (rawIds.isEmpty()) return List.of();

            List<UUID> ids = rawIds.stream()
                    .map(bytes -> {
                        ByteBuffer bb = ByteBuffer.wrap(bytes);
                        return new UUID(bb.getLong(), bb.getLong());
                    })
                    .toList();

            List<Venue> venues = venueRepository.findByIdIn(ids);
            if (venues.isEmpty()) return List.of();

            List<UUID> venueIds = venues.stream().map(Venue::getId).toList();


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
    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
