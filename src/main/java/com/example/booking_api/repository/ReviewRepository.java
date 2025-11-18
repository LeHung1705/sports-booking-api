package com.example.booking_api.repository;

import com.example.booking_api.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    boolean existsByBookingId(UUID bookingId);

    // Lấy page review theo venue (không fetch join để tránh lỗi count query)
    @Query("""
        select r
        from Review r
        where r.venue.id = :venueId
        order by r.createdAt desc
    """)
    Page<Review> findByVenue(@Param("venueId") UUID venueId, Pageable pageable);

    // Top N review mới nhất theo venue (dùng Pageable.of(0,N))
    @Query("""
        select r
        from Review r
        where r.venue.id = :venueId
        order by r.createdAt desc
    """)
    List<Review> findTopByVenue(@Param("venueId") UUID venueId, Pageable pageable);

    // Thống kê rating trung bình & số lượng
    interface ReviewStats {
        Double getAvg();
        Long getCount();
    }

    @Query("""
        select avg(r.rating * 1.0) as avg, count(r.id) as count
        from Review r
        where r.venue.id = :venueId
    """)
    ReviewStats getVenueStats(@Param("venueId") UUID venueId);
}
