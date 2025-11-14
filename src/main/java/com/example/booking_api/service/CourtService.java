package com.example.booking_api.service;

import com.example.booking_api.dto.court.CourtRequest;
import com.example.booking_api.dto.court.CourtResponse;
import com.example.booking_api.entity.Court;
import com.example.booking_api.entity.Venue;
import com.example.booking_api.repository.CourtRepository;
import com.example.booking_api.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CourtService {

    private final CourtRepository courtRepository;
    private final VenueRepository venueRepository;

    // ---------------- CREATE ----------------
    public CourtResponse createCourt(UUID venueId, CourtRequest req) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new RuntimeException("Venue not found"));

        Court court = new Court();
        court.setVenue(venue);
        court.setName(req.getName());
        court.setSport(req.getSport());
        court.setPricePerHour(req.getPricePerHour());
        court.setIsActive(req.getIsActive());

        courtRepository.save(court);
        return mapToResponse(court);
    }

    // ---------------- LIST ----------------
    public List<CourtResponse> getCourts(UUID venueId) {
        return courtRepository.findByVenueId(venueId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ---------------- DETAIL ----------------
    public CourtResponse getCourtById(UUID venueId, UUID courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new RuntimeException("Court not found"));

        if (!court.getVenue().getId().equals(venueId)) {
            throw new RuntimeException("Court does not belong to this venue");
        }

        return mapToResponse(court);
    }

    // ---------------- UPDATE ----------------
    public CourtResponse updateCourt(UUID venueId, UUID courtId, CourtRequest req) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new RuntimeException("Court not found"));

        court.setName(req.getName());
        court.setSport(req.getSport());
        court.setPricePerHour(req.getPricePerHour());
        court.setIsActive(req.getIsActive());

        courtRepository.save(court);
        return mapToResponse(court);
    }

    // ---------------- DELETE ----------------
    public void deleteCourt(UUID venueId, UUID courtId) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new RuntimeException("Court not found"));

        courtRepository.delete(court);
    }

    // ---------------- MAPPING ----------------
    private CourtResponse mapToResponse(Court c) {
        CourtResponse res = new CourtResponse();
        res.setId(c.getId());
        res.setVenueId(c.getVenue().getId());
        res.setName(c.getName());
        res.setSport(c.getSport());
        res.setPricePerHour(c.getPricePerHour());
        res.setIsActive(c.getIsActive());
        return res;
    }
}
