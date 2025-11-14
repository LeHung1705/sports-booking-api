package com.example.booking_api.repository;

import com.example.booking_api.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    interface ReviewStats {
        Long getCount();
        Double getAvg();
    }

    @Query("""
        SELECT COUNT(r) AS count, AVG(r.rating) AS avg
        FROM Review r
        WHERE r.venue.id = :venueId
    """)
    ReviewStats getVenueStats(@Param("venueId") UUID venueId);

    @Query("""
        SELECT r FROM Review r
        JOIN FETCH r.user u
        JOIN FETCH r.court c
        WHERE r.venue.id = :venueId
        ORDER BY r.createdAt DESC
    """)
    List<Review> findTopByVenue(@Param("venueId") UUID venueId, Pageable pageable);
}
