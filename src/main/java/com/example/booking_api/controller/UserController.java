package com.example.booking_api.controller;

import com.example.booking_api.dto.UserProfileResponse;
import com.example.booking_api.dto.UserUpdateRequest;
import com.example.booking_api.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;

    // 🔹 GET /api/v1/users/me
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal String firebaseUid) {
        try {
            UserProfileResponse response = userService.getCurrentUser(firebaseUid);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // 🔹 PUT /api/v1/users/me
    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(@AuthenticationPrincipal String firebaseUid,
                                           @RequestBody UserUpdateRequest request) {
        try {
            UserProfileResponse response = userService.updateProfile(firebaseUid, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}
