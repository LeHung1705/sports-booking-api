package com.example.booking_api.service;

import com.example.booking_api.dto.booking.BookingDetailResponse;
import com.example.booking_api.dto.booking.BookingListRequest;
import com.example.booking_api.dto.booking.BookingListResponse;
import com.example.booking_api.entity.Booking;
import com.example.booking_api.entity.Payment;
import com.example.booking_api.entity.User;
import com.example.booking_api.repository.BookingRepository;
import com.example.booking_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public List<BookingListResponse> listUserBookings(String firebaseUid, BookingListRequest req) {
        try {

            User user = userRepository.findByFirebaseUid(firebaseUid)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Booking> bookings = bookingRepository.findByUserWithFilter(
                    user.getId(),
                    req.getStatus(),
                    req.getFrom(),
                    req.getTo()
            );

            return bookings.stream()
                    .map(b -> BookingListResponse.builder()
                            .id(b.getId())
                            .court(b.getCourt().getName())
                            .startTime(b.getStartTime())
                            .status(b.getStatus() == null ? null : b.getStatus().name())
                            .build())
                    .toList();

        } catch (Exception e) {

            throw new RuntimeException("QUERY_ERROR", e);
        }
    }

    public BookingDetailResponse getBookingDetail(String firebaseUid, UUID bookingId) {
        User me = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Booking b = bookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        boolean isUser = b.getUser() != null && b.getUser().getId().equals(me.getId());
        boolean isOwner = b.getCourt() != null
                && b.getCourt().getVenue() != null
                && b.getCourt().getVenue().getOwner() != null
                && b.getCourt().getVenue().getOwner().getId().equals(me.getId());
        if (!isUser && !isOwner) {
            throw new SecurityException("FORBIDDEN");
        }

        Payment payment = b.getPayment();

        BookingDetailResponse.PaymentItem paymentDto = null;
        if (payment != null) {
            paymentDto = BookingDetailResponse.PaymentItem.builder()
                    .id(payment.getId())
                    .amount(payment.getAmount())
                    .status(payment.getStatus() == null ? null : payment.getStatus().name())
                    .returnPayload(payment.getReturnPayload())
                    .build();
        }

        return BookingDetailResponse.builder()
                .id(b.getId())
                .court(b.getCourt() == null ? null : b.getCourt().getName())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .payment(paymentDto)
                .build();
    }
}