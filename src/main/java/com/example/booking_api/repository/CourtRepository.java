package com.example.booking_api.repository;

import com.example.booking_api.entity.Court;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourtRepository extends JpaRepository<Court, UUID> {
    List<Court> findByVenueId(UUID venueId);
}
