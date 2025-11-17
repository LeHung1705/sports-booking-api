package com.example.booking_api.controller;

import com.example.booking_api.dto.payment.PaymentCreateRequest;
import com.example.booking_api.dto.payment.PaymentCreateResponse;
import com.example.booking_api.service.VnPayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final VnPayService vnpayService;

    @PostMapping("/create")
    public ResponseEntity<?> createPayment(@Valid @RequestBody PaymentCreateRequest request){

        PaymentCreateResponse resp = vnpayService.createPayment(request.getBookingId());
        return ResponseEntity.ok(resp);
    }
}
