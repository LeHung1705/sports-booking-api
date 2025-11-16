<<<<<<< HEAD
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
}
=======
package com.example.booking_api.repository;

import com.example.booking_api.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {}
>>>>>>> origin/feature/voucher-review-notify
