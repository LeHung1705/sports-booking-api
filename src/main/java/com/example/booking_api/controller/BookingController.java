package com.example.booking_api.controller;

import com.example.booking_api.dto.BookingListRequest;
import com.example.booking_api.dto.BookingListResponse;
import com.example.booking_api.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
}
