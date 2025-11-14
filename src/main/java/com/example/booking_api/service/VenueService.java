package com.example.booking_api.service;

import com.example.booking_api.dto.*;
import com.example.booking_api.entity.Review;
import com.example.booking_api.entity.User;
import com.example.booking_api.entity.Venue;
import com.example.booking_api.repository.ReviewRepository;
import com.example.booking_api.repository.UserRepository;
import com.example.booking_api.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VenueService {
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

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

        }catch (Exception e){
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
    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
