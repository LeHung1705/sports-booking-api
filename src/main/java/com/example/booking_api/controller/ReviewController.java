package com.example.booking_api.controller;

import com.example.booking_api.dto.review.*;
import com.example.booking_api.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {

    private final ReviewService reviewService;
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    private String currentUid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? (String) auth.getPrincipal() : null;
    }

    // POST /api/v1/reviews  (AuthGuard: user)
    @PostMapping("/reviews")
    public ResponseEntity<?> create(@RequestBody ReviewCreateRequest req) {
        try {
            String uid = currentUid();
            if (uid == null) return ResponseEntity.status(401).body("Unauthorized");
            var resp = reviewService.create(uid, req);
            return ResponseEntity.status(201).body(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // 400
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(e.getMessage());  // 409 đã review
        }
    }

    // GET /api/v1/venues/{id}/reviews?size=&page=  (public)
    @GetMapping("/venues/{id}/reviews")
    public ResponseEntity<?> list(@PathVariable("id") UUID venueId,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        try {
            PagedResponse<ReviewItemResponse> resp = reviewService.listByVenue(venueId, page, size);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404).body(e.getMessage()); // 404 venue not found
        }
    }
}
