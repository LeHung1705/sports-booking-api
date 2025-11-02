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

    public List<VenueListResponse> searchVenues(VenueListRequest request) {

    }
}
