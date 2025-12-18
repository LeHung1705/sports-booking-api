package com.example.booking_api.service;

import com.example.booking_api.dto.user.UserListResponse;
import com.example.booking_api.entity.User;
import com.example.booking_api.entity.Venue;
import com.example.booking_api.entity.enums.UserRole;
import com.example.booking_api.repository.UserRepository;
import com.example.booking_api.repository.VenueRepository;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ListUsersPage;
import com.google.firebase.auth.UserRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;

    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalVenues", venueRepository.count());
        stats.put("pendingVenues", venueRepository.countByIsActiveFalse());
        return stats;
    }

    public void approveVenue(UUID venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new RuntimeException("Venue not found"));
        venue.setIsActive(true);
        venueRepository.save(venue);
    }

    public List<Venue> getPendingVenues() {
        return venueRepository.findByIsActiveFalse();
    }

    public void updateUserRole(String firebaseUid, UserRole newRole) throws Exception {
        System.out.println("Updating role for " + firebaseUid + " to " + newRole);

        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElse(null);

        if (user == null) {
            // Sync from Firebase
            UserRecord firebaseUser = FirebaseAuth.getInstance().getUser(firebaseUid);
            user = User.builder()
                    .firebaseUid(firebaseUid)
                    .email(firebaseUser.getEmail())
                    .fullName(firebaseUser.getDisplayName())
                    .role(UserRole.USER) // Default before update
                    .build();
        }

        user.setRole(newRole);
        userRepository.save(user);

        // Update Firebase Custom Claims
        Map<String, Object> claims = new HashMap<>(FirebaseAuth.getInstance().getUser(firebaseUid).getCustomClaims());
        claims.put("role", newRole.name());
        FirebaseAuth.getInstance().setCustomUserClaims(firebaseUid, claims);
        System.out.println("Firebase claims updated for " + firebaseUid);
    }

    public List<UserListResponse> getAllUsers() throws Exception {

        List<UserListResponse> users = new ArrayList<>();

        ListUsersPage page = FirebaseAuth.getInstance().listUsers(null);

        for (ExportedUserRecord userRecord : page.iterateAll()) {

            UserListResponse dto = new UserListResponse();
            dto.setUid(userRecord.getUid());
            dto.setEmail(userRecord.getEmail());
            dto.setFullName(userRecord.getDisplayName());

            Object roleClaim = userRecord.getCustomClaims().get("role");
            UserRole role = (roleClaim == null)
                    ? UserRole.USER
                    : UserRole.valueOf(roleClaim.toString().toUpperCase());

            dto.setRole(role);

            users.add(dto);
        }

        return users;
    }
}
