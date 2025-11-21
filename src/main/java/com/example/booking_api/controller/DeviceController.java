package com.example.booking_api.controller;

import com.example.booking_api.service.DeviceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {
    private final DeviceService deviceService;

    private String currentUid() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (String) a.getPrincipal(); // đã set ở FirebaseAuthFilter
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterTokenReq req) {
        if (req.token == null || req.token.isBlank()) {
            return ResponseEntity.badRequest().body("token required");
        }
        deviceService.registerToken(currentUid(), req.token, req.device);
        return ResponseEntity.ok().build();
    }

    // DELETE theo PathVariable (chú ý URL-encode nếu token có ký tự đặc biệt)
    @DeleteMapping("/{token}")
    public ResponseEntity<?> unregister(@PathVariable String token) {
        long deleted = deviceService.unregisterToken(currentUid(), token);
        return (deleted > 0) ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    // (Tuỳ chọn) DELETE nhận token qua body để tránh lỗi URL-encode
    @DeleteMapping("/unregister")
    public ResponseEntity<?> unregisterBody(@RequestBody UnregisterTokenReq req) {
        if (req.token == null || req.token.isBlank()) {
            return ResponseEntity.badRequest().body("token required");
        }
        long deleted = deviceService.unregisterToken(currentUid(), req.token);
        return (deleted > 0) ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @Data public static class RegisterTokenReq { public String token; public String device; }
    @Data public static class UnregisterTokenReq { public String token; }
}
