package com.example.booking_api.controller;


import com.example.booking_api.dto.NotificationResponse;
import com.example.booking_api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Lấy firebaseUid từ SecurityContext (đã set trong FirebaseAuthFilter)
    private String getCurrentFirebaseUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (String) authentication.getPrincipal();
    }

    // GET /api/v1/notifications?read=true/false
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
            @RequestParam(value = "read", required = false) Boolean read
    ) {
        String firebaseUid = getCurrentFirebaseUid();
        List<NotificationResponse> responses =
                notificationService.getNotificationsForUser(firebaseUid, read);

        return ResponseEntity.ok(responses);
    }

    // PUT /api/v1/notifications/{id}/read
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable("id") UUID id) {
        String firebaseUid = getCurrentFirebaseUid();

        NotificationResponse response =
                notificationService.markAsRead(firebaseUid, id);

        return ResponseEntity.ok(response);
    }
}
