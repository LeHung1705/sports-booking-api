package com.example.booking_api.repository;

import com.example.booking_api.entity.Venue;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VenueRepository extends JpaRepository<Venue, UUID> {
    @Query(value = """
        SELECT DISTINCT v.id
        FROM venues v
        LEFT JOIN courts c ON c.venue_id = v.id
        WHERE (:city IS NULL OR v.city = :city)
          AND (:sport IS NULL OR c.sport = :sport)
          AND (
               :q IS NULL OR
               LOWER(v.name)    LIKE CONCAT('%', LOWER(:q), '%') OR
               LOWER(v.address) LIKE CONCAT('%', LOWER(:q), '%')
          )
          AND (
               :lat IS NULL OR :lng IS NULL OR :radius IS NULL OR
               ( 6371 * 2 * ASIN(SQRT(
                    POWER(SIN(RADIANS(:lat - v.latitude) / 2), 2) +
                    COS(RADIANS(v.latitude)) * COS(RADIANS(:lat)) *
                    POWER(SIN(RADIANS(:lng - v.longitude) / 2), 2)
               ))) <= :radius
          )
        """,
            nativeQuery = true)
    List<byte[]> findIds(
            @Param("q") String q,
            @Param("city") String city,
            @Param("sport") String sport,
            @Param("lat") Double lat,
            @Param("lng") Double lng,
            @Param("radius") Double radiusKm
    );

    @EntityGraph(attributePaths = "courts")
    List<Venue> findByIdIn(List<UUID> ids);

    @EntityGraph(attributePaths = {"courts"})
    Optional<Venue> findWithCourtsById(UUID id);
}