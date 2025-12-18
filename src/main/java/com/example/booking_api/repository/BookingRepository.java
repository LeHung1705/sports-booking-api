package com.example.booking_api.repository;

import com.example.booking_api.entity.Booking;
import com.example.booking_api.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    @Query("""
        SELECT b
        FROM Booking b
        JOIN FETCH b.court c
        WHERE b.user.id = :userId
          AND (:statuses IS NULL OR b.status IN :statuses)
          AND (:from IS NULL OR b.startTime >= :from)
          AND (:to IS NULL OR b.startTime <= :to)
        ORDER BY b.startTime DESC
    """)
    List<Booking> findByUserWithFilter(
            @Param("userId") UUID userId,
            @Param("statuses") List<BookingStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
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
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        SELECT b FROM Booking b
        WHERE b.court.venue.id = :venueId
          AND b.status <> com.example.booking_api.entity.enums.BookingStatus.CANCELED
          AND b.endTime > :startTime
          AND b.startTime < :endTime
    """)
    List<Booking> findByVenueAndDateRange(
            @Param("venueId") UUID venueId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        SELECT b FROM Booking b
        WHERE b.court.id = :courtId
          AND b.status <> com.example.booking_api.entity.enums.BookingStatus.CANCELED
          AND b.endTime > :startTime
          AND b.startTime < :endTime
    """)
    List<Booking> findByCourtAndDateRange(
            @Param("courtId") UUID courtId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query("""
        SELECT b
        FROM Booking b
        JOIN FETCH b.court c
        JOIN FETCH c.venue v
        WHERE v.owner.id = :ownerId
        ORDER BY b.startTime DESC
    """)
    List<Booking> findByOwner(@Param("ownerId") UUID ownerId);

    @Query("""
        SELECT b
        FROM Booking b
        JOIN FETCH b.court c
        JOIN FETCH c.venue v
        WHERE v.owner.id = :ownerId
          AND b.status = :status
        ORDER BY b.startTime ASC
    """)
    List<Booking> findByOwnerAndStatus(@Param("ownerId") UUID ownerId, @Param("status") BookingStatus status);

    @Query("""
        SELECT b
        FROM Booking b
        JOIN FETCH b.court c
        JOIN FETCH c.venue v
        WHERE v.owner.id = :ownerId
          AND (:venueId IS NULL OR v.id = :venueId)
          AND (:statuses IS NULL OR b.status IN :statuses)
          AND (:from IS NULL OR b.startTime >= :from)
          AND (:to IS NULL OR b.startTime <= :to)
        ORDER BY b.startTime DESC
    """)
    List<Booking> findByOwnerWithFilter(
            @Param("ownerId") UUID ownerId,
            @Param("venueId") UUID venueId,
            @Param("statuses") List<BookingStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    List<Booking> findByStatusAndCreatedAtBefore(BookingStatus status, LocalDateTime createdAt);
}
