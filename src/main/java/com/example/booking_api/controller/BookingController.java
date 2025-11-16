package com.example.booking_api.controller;

import com.example.booking_api.dto.booking.BookingDetailResponse;
import com.example.booking_api.dto.booking.BookingListRequest;
import com.example.booking_api.dto.booking.BookingListResponse;
import com.example.booking_api.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {
    private final BookingService bookingService;

    @GetMapping
    public ResponseEntity<?> listUerBookings(@AuthenticationPrincipal String firebaseUid, BookingListRequest request){
        try {
            List<BookingListResponse> data = bookingService.listUserBookings(firebaseUid, request);
            return ResponseEntity.ok().body(data);
        } catch (Exception ex) {
            String msg = ex.getMessage();
            if ("User not found".equals(msg)) {
                return ResponseEntity.status(401).body(Map.of(
                        "message", "User not found"
                ));
            }
            if ("QUERY_ERROR".equals(msg)) {
                return ResponseEntity.status(500).body(Map.of(
                        "message", "Lỗi truy vấn"
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                    "message", msg
            ));
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookingById(@AuthenticationPrincipal String firebaseUid, @PathVariable UUID id){
        try {
            BookingDetailResponse body = bookingService.getBookingDetail(firebaseUid, id);
            return ResponseEntity.ok(body);

        } catch (SecurityException e) {

            return ResponseEntity.status(403).body(Map.of(
                    "message", "Truy cập trái phép"
            ));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if ("User not found".equals(msg)) {
                return ResponseEntity.status(401).body(Map.of(
                        "message", "User not found"
                ));
            }
            if ("Booking not found".equals(msg)) {
                return ResponseEntity.status(404).body(Map.of(
                        "message", "Booking not found"
                ));
            }
            return ResponseEntity.badRequest().body(Map.of(
                    "message", msg
            ));
        }
    }
}
