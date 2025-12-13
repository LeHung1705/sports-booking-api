package com.example.booking_api.controller;


import com.example.booking_api.dto.venue.*;
import com.example.booking_api.service.VenueService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
public class VenueController {
    private final VenueService venueService;

    @PostMapping
    public ResponseEntity<?> createVenue(@AuthenticationPrincipal String firebaseUid, @Valid @RequestBody VenueCreateRequest request) {
        try {
            VenueResponse venue = venueService.createVenue(firebaseUid, request);
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
        System.out.println("DEBUG: VenueController.listVenues called with: " + request);
        try {
            List<VenueListResponse> venues = venueService.searchVenues(request);
            return ResponseEntity.ok(venues);
        } catch (RuntimeException e) {
            e.printStackTrace(); // Print stack trace to see the error
            return ResponseEntity.status(500).body(Map.of("message", "Query error"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getVenueById(@PathVariable UUID id) {
        try {
            VenueDetailResponse body = venueService.getVenueDetail(id);
            return ResponseEntity.ok(body);
        }catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "message", "Không tìm thấy sân"
            ));
        }
    }

    @GetMapping("/{id}/availability")
    public ResponseEntity<?> getAvailability(
            @PathVariable UUID id, 
            @RequestParam("date") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date
    ) {
        try {
            return ResponseEntity.ok(venueService.getVenueAvailability(id, date));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    // 👇 [BỔ SUNG HÀM NÀY]
    @GetMapping("/my-venues")
    public ResponseEntity<?> getMyVenues(@AuthenticationPrincipal String firebaseUid) {
        try {
            List<VenueResponse> venues = venueService.getMyVenues(firebaseUid);
            return ResponseEntity.ok(venues);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateVenue(@AuthenticationPrincipal String firebaseUid, @PathVariable UUID id, @Valid @RequestBody VenueUpdateRequest request) {
        try {
            VenueResponse updated = venueService.updateVenue(firebaseUid, id, request);
            return ResponseEntity.ok(updated);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "message", "You are not allowed to update this venue"));
        } catch (RuntimeException e) {

            String msg = e.getMessage();
            if ("Venue not found".equals(msg)) {
                return ResponseEntity.status(404).body(Map.of(
                        "message", "Venue not found"
                ));
            }

            if ("User not found".equals(msg)) {
                return ResponseEntity.status(401).body(Map.of(
                        "message", "User not found"
                ));
            }

            return ResponseEntity.badRequest().body(Map.of(
                    "message", msg
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteVenue(@AuthenticationPrincipal String firebaseUid, @PathVariable UUID id) {
        try {
            venueService.deleteVenue(firebaseUid, id);
            return ResponseEntity.ok(Map.of(
                    "message", "Deleted"
            ));
        }catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "message", "You are not allowed to delete this venue"
            ));

        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if ("Venue not found".equals(msg)) {
                return ResponseEntity.status(404).body(Map.of(
                        "message", "Venue not found"
                ));
            }
            if ("User not found".equals(msg)) {
                return ResponseEntity.status(401).body(Map.of(
                        "message", "User not found"
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                    "message", msg
            ));
        }
    }
}
