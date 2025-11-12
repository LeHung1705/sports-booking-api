package com.example.booking_api.service;

import com.example.booking_api.dto.BookingListRequest;
import com.example.booking_api.dto.BookingListResponse;
import com.example.booking_api.entity.Booking;
import com.example.booking_api.entity.User;
import com.example.booking_api.repository.BookingRepository;
import com.example.booking_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
}