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

import com.example.booking_api.dto.booking.TimeSlotResponse;
import com.example.booking_api.service.BookingService;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/venues/{venueId}/courts")
@RequiredArgsConstructor
public class CourtController {

    private final CourtService courtService;
    private final BookingService bookingService;

    // 1. Method từ nhánh MAIN: Lấy danh sách Court
    @GetMapping
    public ResponseEntity<List<CourtResponse>> getCourts(
            @PathVariable UUID venueId
    ) {
        return ResponseEntity.ok(courtService.getCourts(venueId));
    }

    // 2. Method từ nhánh MAIN: Lấy chi tiết 1 Court
    // (Tôi đã bổ sung phần body vì nó bị cắt mất trong conflict)
    @GetMapping("/{courtId}")
    public ResponseEntity<CourtResponse> getCourt(
            @PathVariable UUID venueId,
            @PathVariable UUID courtId
    ) {
        return ResponseEntity.ok(courtService.getCourtById(venueId, courtId));
    }

    // 3. Method từ nhánh TEST-FEAT: Xem lịch trống (Availability)
    @PreAuthorize("hasAnyRole('USER', 'OWNER')")
    @GetMapping("/{courtId}/availability")
    public ResponseEntity<List<TimeSlotResponse>> getAvailability(
            @PathVariable UUID venueId,
            @PathVariable UUID courtId,
            @RequestParam("date") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        // Ideally check if court belongs to venueId
        return ResponseEntity.ok(bookingService.getAvailability(courtId, date));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public ResponseEntity<CourtResponse> createCourt(
            @PathVariable UUID venueId,
            @Valid @RequestBody CourtRequest request
    ) {
        return ResponseEntity.ok(courtService.createCourt(venueId, request));
    }

    // Các method Update và Delete giữ nguyên
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