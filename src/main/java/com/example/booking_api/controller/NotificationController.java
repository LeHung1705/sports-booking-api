package com.example.booking_api.controller;

import com.example.booking_api.dto.NotificationResponse;
import com.example.booking_api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * GET /api/v1/notifications?read=true|false
     *
     * - read = null  → tất cả
     * - read = true  → chỉ đã đọc
     * - read = false → chỉ chưa đọc
     *
     * Middleware: đã được FirebaseAuthFilter + SecurityConfig handle rồi
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestParam(value = "read", required = false) Boolean read
    ) {
        String currentUid = getCurrentFirebaseUid();
        List<NotificationResponse> responses =
                notificationService.getNotificationsForUser(currentUid, read);

        return ResponseEntity.ok(responses);
    }

    // Lấy Firebase UID từ SecurityContext
    private String getCurrentFirebaseUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("Unauthenticated");
        }
        // FirebaseAuthFilter đang set principal = uid (String)
        return auth.getName(); // hoặc (String) auth.getPrincipal();
    }

    // (Optional) nếu sau này muốn thêm API đánh dấu đã đọc:
    /*
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        String currentUid = getCurrentFirebaseUid();
        notificationService.markAsRead(id, currentUid);
        return ResponseEntity.noContent().build();
    }
    */
}