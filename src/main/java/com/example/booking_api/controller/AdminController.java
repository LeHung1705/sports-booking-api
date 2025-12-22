package com.example.booking_api.controller;

import com.example.booking_api.dto.user.SetRoleRequest;
import com.example.booking_api.entity.enums.UserRole;
import com.example.booking_api.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

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

    // GET /admin/stats
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        return ResponseEntity.ok(adminService.getStats());
    }

    // GET /admin/venues/pending
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/venues/pending")
    public ResponseEntity<?> getPendingVenues() {
        return ResponseEntity.ok(adminService.getPendingVenues());
    }

    // PUT /admin/venues/{id}/approve
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/venues/{id}/approve")
    public ResponseEntity<?> approveVenue(@PathVariable UUID id) {
        try {
            adminService.approveVenue(id);
            return ResponseEntity.ok("Venue approved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    // PUT /admin/venues/{id}/reject
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/venues/{id}/reject")
    public ResponseEntity<?> rejectVenue(@PathVariable UUID id) {
        try {
            adminService.rejectVenue(id);
            return ResponseEntity.ok("Venue rejected successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    // PUT /admin/users/{id}/upgrade
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/upgrade")
    public ResponseEntity<?> upgradeUser(@PathVariable String id) {
        try {
            adminService.updateUserRole(id, UserRole.OWNER);
            return ResponseEntity.ok("User upgraded to OWNER");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    // PUT /admin/users/{id}/degrade
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/users/{id}/degrade")
    public ResponseEntity<?> degradeUser(@PathVariable String id) {
        try {
            adminService.updateUserRole(id, UserRole.USER);
            return ResponseEntity.ok("Owner degraded to USER");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }
}
