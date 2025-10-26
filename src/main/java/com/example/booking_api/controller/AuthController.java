package com.example.booking_api.controller;

import com.example.booking_api.dto.LoginRequest;
import com.example.booking_api.dto.LoginResponse;
import com.example.booking_api.dto.RegisterRequest;
import com.example.booking_api.dto.RegisterResponse;
import com.example.booking_api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            RegisterResponse response = authService.register(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Lỗi hệ thống: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (RuntimeException e) {
            if (e.getMessage().contains("401"))
                return ResponseEntity.status(401).body("Sai thông tin đăng nhập");
            else
                return ResponseEntity.badRequest().body("Thiếu dữ liệu hoặc lỗi khác");
        }
    }
}
