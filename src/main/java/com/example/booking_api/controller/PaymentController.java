package com.example.booking_api.controller;

import com.example.booking_api.dto.payment.PaymentCreateRequest;
import com.example.booking_api.dto.payment.PaymentCreateResponse;
import com.example.booking_api.dto.payment.VnPayReturnResponse;
import com.example.booking_api.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(String firebaseUid, @Valid @RequestBody PaymentCreateRequest request, HttpServletRequest httpRequest){
        try {
            firebaseUid = "user-111";
            PaymentCreateResponse res = paymentService.createVnPayPayment(firebaseUid, request, httpRequest);
            return ResponseEntity.ok(res);
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of(
                    "message", "Bạn không được phép thanh toán booking này"
            ));
        } catch (RuntimeException e) {
            String msg = e.getMessage();
            if ("User not found".equals(msg)) {
                return ResponseEntity.status(401).body(Map.of("message", "User not found"));
            }
            if ("Booking not found".equals(msg)) {
                return ResponseEntity.status(404).body(Map.of("message", "Booking not found"));
            }
            if ("BOOKING_STATUS_NOT_ALLOWED".equals(msg)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Trạng thái đơn không cho phép thanh toán"
                ));
            }
            if ("BOOKING_AMOUNT_INVALID".equals(msg)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "message", "Số tiền thanh toán không hợp lệ"
                ));
            }
            return ResponseEntity.badRequest().body(Map.of("message", msg));
        }
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> vnpParams) {
        try {
            VnPayReturnResponse res = paymentService.handleVnPayReturn(vnpParams);

            return ResponseEntity.ok(res);

        } catch (RuntimeException e) {

            return ResponseEntity.badRequest().body(Map.of(
                    "message", e.getMessage()
            ));
        }
    }

}
