package com.example.booking_api.controller;


import com.example.booking_api.dto.VenueCreateRequest;
import com.example.booking_api.dto.VenueCreateResponse;
import com.example.booking_api.dto.VenueListRequest;
import com.example.booking_api.dto.VenueListResponse;
import com.example.booking_api.service.VenueService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {
    private final VenueService venueService;

    @PostMapping
    public ResponseEntity<?> createVenue(@AuthenticationPrincipal String firebaseUid, @Valid @RequestBody VenueCreateRequest request) {
        try {
            VenueCreateResponse venue = venueService.create(firebaseUid, request);
            return ResponseEntity.status(201).body(venue);
        }catch (ConstraintViolationException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", "Invalid data",
                    "details", e.getMessage()
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "message", "You are not allowed to create a venue"
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<?> listVenues(@Valid VenueListRequest request) {
        try {
            List<VenueListResponse> venues = venueService.search(request);
            return ResponseEntity.ok(venues);
        } catch (RuntimeException e) {
            return ResponseEntity.status(500).body(Map.of("message", "Query error"));
        }
    }
}
