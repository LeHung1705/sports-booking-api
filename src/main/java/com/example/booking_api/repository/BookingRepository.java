package com.example.booking_api.repository;

import com.example.booking_api.entity.Booking;
import com.example.booking_api.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("""
        SELECT b
        FROM Booking b
        JOIN FETCH b.court c
        WHERE b.user.id = :userId
          AND (:status IS NULL OR b.status = :status)
          AND (:from IS NULL OR b.startTime >= :from)
          AND (:to IS NULL OR b.startTime <= :to)
        ORDER BY b.startTime DESC
    """)
    List<Booking> findByUserWithFilter(
            @Param("userId") UUID userId,
            @Param("status") BookingStatus status,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );


    @Query("""
        SELECT b FROM Booking b
        JOIN FETCH b.court c
        JOIN FETCH c.venue v
        LEFT JOIN FETCH b.payment p
        WHERE b.id = :id
    """)
    Optional<Booking> findDetailById(@Param("id") UUID id);

    @Query("""
        SELECT COUNT(b) FROM Booking b
        WHERE b.court.id = :courtId
          AND b.status <> com.example.booking_api.entity.enums.BookingStatus.CANCELED
          AND b.startTime < :endTime
          AND b.endTime > :startTime
    """)
    long countOverlappingBookings(
            @Param("courtId") UUID courtId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    @Query("""
        SELECT b FROM Booking b
        WHERE b.court.venue.id = :venueId
          AND b.status <> com.example.booking_api.entity.enums.BookingStatus.CANCELED
          AND b.startTime >= :startTime
          AND b.endTime <= :endTime
    """)
    List<Booking> findByVenueAndDateRange(
            @Param("venueId") UUID venueId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    @Query("""
        SELECT b FROM Booking b
        WHERE b.court.id = :courtId
          AND b.status <> com.example.booking_api.entity.enums.BookingStatus.CANCELED
          AND b.startTime >= :startTime
          AND b.endTime <= :endTime
    """)
    List<Booking> findByCourtAndDateRange(
            @Param("courtId") UUID courtId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );
}
