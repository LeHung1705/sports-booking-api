package com.example.booking_api.service;

import com.example.booking_api.dto.VenueCreateRequest;
import com.example.booking_api.dto.VenueCreateResponse;
import com.example.booking_api.dto.VenueListRequest;
import com.example.booking_api.dto.VenueListResponse;
import com.example.booking_api.entity.User;
import com.example.booking_api.entity.Venue;
import com.example.booking_api.repository.UserRepository;
import com.example.booking_api.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VenueService {
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;

    public VenueCreateResponse create(String firebaseUid, VenueCreateRequest req) {
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

        return VenueCreateResponse.builder()
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

    public List<VenueListResponse> search(VenueListRequest req) {
        try {
            List<Object> rawIds = venueRepository.findIds(
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
                    .map(o -> UUID.fromString(o.toString()))
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

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
