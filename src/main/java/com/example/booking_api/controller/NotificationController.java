package com.example.booking_api.controller;

import com.example.booking_api.service.NotificationService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // 1. Đăng ký Token (Khi Login) - Thay thế cho DeviceController.register
    @PostMapping("/register")
    public ResponseEntity<?> registerToken(@RequestBody RegisterTokenRequest req) {
        // Lấy Email của user đang đăng nhập từ Token
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        notificationService.registerToken(email, req.getToken(), req.getDeviceType());

        return ResponseEntity.ok(Map.of("message", "Token registered successfully"));
    }

    // 2. Hủy Token (Khi Logout) - Thay thế cho DeviceController.unregister
    @PostMapping("/unregister")
    public ResponseEntity<?> unregisterToken(@RequestBody UnregisterTokenRequest req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();

        notificationService.unregisterToken(email, req.getToken());

        return ResponseEntity.ok(Map.of("message", "Token unregistered"));
    }

    // 3. Test gửi thông báo (Giữ nguyên)
    @PostMapping("/test-send")
    public ResponseEntity<?> sendTestNotification(@RequestBody SendRequest req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        notificationService.sendNotificationToUser(email, req.getTitle(), req.getBody());
        return ResponseEntity.ok(Map.of("message", "Sent"));
    }

    // --- DTO Classes ---
    @Data
    public static class RegisterTokenRequest {
        private String token;
        private String deviceType; // "android" hoặc "ios"
    }

    @Data
    public static class UnregisterTokenRequest {
        private String token;
    }

    @Data
    public static class SendRequest {
        private String title;
        private String body;
    }
}