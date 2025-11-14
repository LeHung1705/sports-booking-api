package com.example.booking_api.controller;

import com.example.booking_api.dto.user.UserProfileResponse;
import com.example.booking_api.dto.user.UserUpdateRequest;
import com.example.booking_api.dto.user.SetRoleRequest;
import com.example.booking_api.repository.UserRepository;
import com.example.booking_api.service.UserService;
import com.google.firebase.auth.FirebaseAuth;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal String firebaseUid) {
        try {
            UserProfileResponse response = userService.getCurrentUser(firebaseUid);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

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
