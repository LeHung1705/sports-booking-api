package com.example.booking_api.service;

import com.example.booking_api.dto.review.CourtReviewResponse;
import com.example.booking_api.dto.review.PagedResponse;
import com.example.booking_api.dto.review.ReviewCreateRequest;
import com.example.booking_api.dto.review.ReviewItemResponse;
import com.example.booking_api.entity.Booking;
import com.example.booking_api.entity.Court;
import com.example.booking_api.entity.Review;
import com.example.booking_api.entity.User;
import com.example.booking_api.entity.Venue;
import com.example.booking_api.entity.enums.BookingStatus;
import com.example.booking_api.repository.BookingRepository;
import com.example.booking_api.repository.ReviewRepository;
import com.example.booking_api.repository.UserRepository;
import com.example.booking_api.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final VenueRepository venueRepository;

    @Transactional
    public ReviewItemResponse create(String firebaseUid, ReviewCreateRequest req) {
        // validate input
        if (req.getBookingId() == null || req.getRating() == null) {
            throw new IllegalArgumentException("bookingId & rating are required");
        }
        if (req.getRating() < 1 || req.getRating() > 5) {
            throw new IllegalArgumentException("rating must be 1..5");
        }

        // user từ token
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // booking + fetch court/venue (findDetailById nên fetch join court -> venue)
        Booking booking = bookingRepository.findDetailById(req.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // booking phải thuộc user hiện tại
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Booking does not belong to current user");
        }

        // booking đã hoàn tất
        if (booking.getStatus() != BookingStatus.COMPLETED) {
            throw new IllegalArgumentException("Booking is not completed");
        }

        // 1 booking chỉ 1 review
        if (reviewRepository.existsByBookingId(booking.getId())) {
            throw new IllegalStateException("Booking already reviewed");
        }

        // bắt buộc có court & venue
        Court court = booking.getCourt();
        if (court == null) {
            throw new IllegalStateException("Booking missing court");
        }
        Venue venue = court.getVenue();
        if (venue == null) {
            throw new IllegalStateException("Court missing venue");
        }

        // tạo review (NHỚ set court & venue)
        Review saved = reviewRepository.save(
                Review.builder()
                        .booking(booking)
                        .user(user)
                        .court(court)                          // <<< quan trọng
                        .venue(venue)                          // <<< quan trọng (nếu bảng reviews có cột venue_id)
                        .rating(req.getRating().shortValue())  // entity dùng Short
                        .comment(req.getComment())
                        .build()
        );

        // trả response
        return new ReviewItemResponse(
                saved.getId(),
                saved.getRating() == null ? null : saved.getRating().intValue(),
                saved.getComment(),
                user.getFullName(),
                saved.getCreatedAt()
        );
    }

    public PagedResponse<ReviewItemResponse> listByVenue(UUID venueId, int page, int size) {
        if (!venueRepository.existsById(venueId)) {
            throw new IllegalArgumentException("Venue not found");
        }

        Page<Review> p = reviewRepository.findByVenue(venueId,
                PageRequest.of(Math.max(0, page), Math.max(1, size)));

        List<ReviewItemResponse> items = p.getContent().stream()
                .map(r -> new ReviewItemResponse(
                        r.getId(),
                        r.getRating() == null ? null : r.getRating().intValue(),
                        r.getComment(),
                        r.getUser() != null ? r.getUser().getFullName() : null,
                        r.getCreatedAt()
                ))
                .toList();

        return new PagedResponse<>(
                items,
                p.getNumber(),
                p.getSize(),
                p.getTotalElements(),
                p.getTotalPages()
        );
    }

    public Page<CourtReviewResponse> getReviewsByCourt(UUID courtId, Pageable pageable) {
        Page<Review> reviews = reviewRepository.findByCourtId(courtId, pageable);
        return reviews.map(review -> {
            CourtReviewResponse res = new CourtReviewResponse();
            res.setId(review.getId());
            res.setRating(review.getRating());
            res.setComment(review.getComment());
            res.setUserName(review.getUser().getFullName());
            res.setCreatedAt(review.getCreatedAt());
            return res;
        });
    }
}
