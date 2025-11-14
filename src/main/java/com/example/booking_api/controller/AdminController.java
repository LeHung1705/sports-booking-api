package com.example.booking_api.controller;

import com.example.booking_api.dto.user.SetRoleRequest;
import com.example.booking_api.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminController {

    private final AdminService adminService;

    // PATCH /admin/users/{uid}/role
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/users/{uid}/role")
    public ResponseEntity<?> setUserRole(
            @PathVariable String uid,
            @RequestBody SetRoleRequest request
    ) {
        try {
            adminService.updateUserRole(uid, request.getRole());
            return ResponseEntity.ok("Role updated to: " + request.getRole());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    // GET /admin/users
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        try {
            return ResponseEntity.ok(adminService.getAllUsers());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
}
