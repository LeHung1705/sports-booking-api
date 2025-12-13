package com.example.booking_api.controller;

import com.example.booking_api.dto.*;
import com.example.booking_api.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/test")
    public String test() {
        return "Backend is running!";
    }

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

    /**
     * POST /api/v1/auth/forgot-password
     * Gửi email reset mật khẩu
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            ForgotPasswordResponse response = authService.forgotPassword(request);
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // 400 Bad Request - Thiếu dữ liệu hoặc email không hợp lệ
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));

        } catch (RuntimeException e) {
            if (e.getMessage().contains("chưa được đăng ký")) {
                // 404 Not Found - Email không tồn tại
                return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
            }
            // 500 Internal Server Error - Lỗi Firebase hoặc hệ thống
            return ResponseEntity.status(500).body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        try {
            LogoutResponse response = authService.logout();
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Lỗi khi đăng xuất: " + e.getMessage()));
        }
    }
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest request) {
        try {
            // Bây giờ nó đã nhìn thấy 'authService' vì nằm cùng trong class
            authService.changePassword(request);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }
    // -----------------------------------------------------------------------

} // <--- ĐÂY LÀ DẤU ĐÓNG NGOẶC CỦA CLASS (End of File)

