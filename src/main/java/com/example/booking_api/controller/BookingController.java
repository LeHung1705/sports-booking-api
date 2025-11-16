package com.example.booking_api.controller;

import com.example.booking_api.dto.booking.*;
import com.example.booking_api.service.BookingService;
import jakarta.validation.Valid;
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
    @PostMapping
    public ResponseEntity<?> createBooking(@AuthenticationPrincipal String firebaseUid, @Valid @RequestBody BookingCreateRequest request) {
        try {
            BookingCreateResponse res = bookingService.createBooking(firebaseUid, request);
            return ResponseEntity.status(201).body(res);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage()
            ));
        } catch (RuntimeException e) {
            String msg = e.getMessage();

            if ("User not found".equals(msg)) {
                return ResponseEntity.status(401).body(Map.of(
                        "message", "User not found"
                ));
            }
            if ("Court not found".equals(msg)) {
                return ResponseEntity.status(404).body(Map.of(
                        "message", "Court not found"
                ));
            }

            if ("TIME_OVERLAP".equals(msg)) {
                // 409 trùng giờ
                return ResponseEntity.status(409).body(Map.of(
                        "message", "Time slot already booked"
                ));
            }

            return ResponseEntity.badRequest().body(Map.of(
                    "message", msg
            ));
        }
    }

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

    @PutMapping("/{id}/apply-voucher")
    public ResponseEntity<?> applyVoucher(@AuthenticationPrincipal String firebaseUid, @PathVariable UUID id, @Valid @RequestBody BookingApplyVoucherRequest request) {
        try {
            BookingApplyVoucherResponse res = bookingService.applyVoucher(firebaseUid, id, request);
            return ResponseEntity.ok(res);

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "message", "Bạn không được phép áp dụng voucher cho đơn này"
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

            if ("VOUCHER_NOT_FOUND".equals(msg)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Voucher không hợp lệ"
                ));
            }

            if ("VOUCHER_EXPIRED".equals(msg)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Voucher đã hết hạn hoặc không còn hiệu lực"
                ));
            }

            if ("VOUCHER_ALREADY_APPLIED".equals(msg)) {
                return ResponseEntity.status(409).body(Map.of(
                        "message", "Đơn đặt sân này đã áp dụng voucher"
                ));
            }

            if ("MIN_ORDER_NOT_REACHED".equals(msg)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Đơn hàng không đủ điều kiện sử dụng voucher"
                ));
            }

            if ("BOOKING_STATUS_NOT_ALLOWED".equals(msg)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Trạng thái đơn không cho phép áp dụng voucher"
                ));
            }

            return ResponseEntity.badRequest().body(Map.of(
                    "message", msg
            ));
        }
    }

    @PutMapping("/{id}/remove-voucher")
    public ResponseEntity<?> removeVoucher(@AuthenticationPrincipal String firebaseUid, @PathVariable UUID id) {
        try {
            return ResponseEntity.ok(bookingService.removeVoucher(firebaseUid, id));

        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("message", "Không có quyền gỡ voucher"));

        } catch (RuntimeException e) {
            if ("Booking not found".equals(e.getMessage())) {
                return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy booking"));
            }
            if ("NO_VOUCHER_APPLIED".equals(e.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Booking chưa áp voucher"));
            }
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@AuthenticationPrincipal String firebaseUid, @PathVariable UUID id, @Valid @RequestBody BookingCancelRequest request) {
        try {
            BookingCancelResponse res = bookingService.cancelBooking(firebaseUid, id, request);

            return ResponseEntity.ok(Map.of(
                    "status", res.getStatus().name()
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "message", "Bạn không có quyền huỷ đơn này"
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

            if ("CANCEL_WINDOW_PASSED".equals(msg)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Đơn đã quá hạn huỷ"
                ));
            }

            if ("PAYMENT_IN_PROGRESS".equals(msg)) {
                return ResponseEntity.status(409).body(Map.of(
                        "message", "Đơn đang trong quá trình thanh toán, không thể huỷ"
                ));
            }

            if ("ALREADY_CANCELED".equals(msg)) {

                return ResponseEntity.status(409).body(Map.of(
                        "message", "Đơn đã được huỷ trước đó"
                ));
            }

            return ResponseEntity.badRequest().body(Map.of(
                    "message", msg
            ));
        }
    }
}
