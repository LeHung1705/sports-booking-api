package com.example.booking_api.controller;

import com.example.booking_api.dto.court.CourtRequest;
import com.example.booking_api.dto.court.CourtResponse;
import com.example.booking_api.service.CourtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/venues/{venueId}/courts")
@RequiredArgsConstructor
public class CourtController {

    private final CourtService courtService;

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public ResponseEntity<CourtResponse> createCourt(
            @PathVariable UUID venueId,
            @Valid @RequestBody CourtRequest request
    ) {
        return ResponseEntity.ok(courtService.createCourt(venueId, request));
    }

    @PreAuthorize("hasAnyRole('USER', 'OWNER')")
    @GetMapping
    public ResponseEntity<List<CourtResponse>> getCourts(
            @PathVariable UUID venueId
    ) {
        return ResponseEntity.ok(courtService.getCourts(venueId));
    }

    @PreAuthorize("hasAnyRole('USER', 'OWNER')")
    @GetMapping("/{courtId}")
    public ResponseEntity<CourtResponse> getCourt(
            @PathVariable UUID venueId,
            @PathVariable UUID courtId
    ) {
        return ResponseEntity.ok(courtService.getCourtById(venueId, courtId));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{courtId}")
    public ResponseEntity<CourtResponse> updateCourt(
            @PathVariable UUID venueId,
            @PathVariable UUID courtId,
            @Valid @RequestBody CourtRequest request
    ) {
        return ResponseEntity.ok(courtService.updateCourt(venueId, courtId, request));
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{courtId}")
    public ResponseEntity<Void> deleteCourt(
            @PathVariable UUID venueId,
            @PathVariable UUID courtId
    ) {
        courtService.deleteCourt(venueId, courtId);
        return ResponseEntity.noContent().build();
    }
}
