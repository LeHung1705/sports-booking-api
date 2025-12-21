package com.example.booking_api.service;

import com.example.booking_api.dto.user.UserProfileResponse;
import com.example.booking_api.dto.user.UserUpdateRequest;
import com.example.booking_api.entity.User;
import com.example.booking_api.entity.Venue;
import com.example.booking_api.entity.enums.UserRole;
import com.example.booking_api.repository.UserRepository;
import com.example.booking_api.repository.VenueRepository;
import com.example.booking_api.repository.CourtRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final VenueRepository venueRepository;
    private final CourtRepository courtRepository;

    public UserProfileResponse getCurrentUser(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Map<String, Object> stats = new HashMap<>();
        if (user.getRole() == UserRole.OWNER) {
            long count = courtRepository.countByVenue_Owner_Id(user.getId());
            stats.put("activeCourts", count); 
        }

        return UserProfileResponse.builder()
                .id(user.getId().toString())
                .full_name(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().toString())
                .stats(stats)
                .build();
    }

    public UserProfileResponse updateProfile(String firebaseUid, UserUpdateRequest req) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (req.getFull_name() != null && !req.getFull_name().isEmpty())
            user.setFullName(req.getFull_name());
        if (req.getPhone() != null && !req.getPhone().isEmpty())
            user.setPhone(req.getPhone());

        userRepository.save(user);

        return UserProfileResponse.builder()
                .id(user.getId().toString())
                .full_name(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().toString())
                .build();
    }
}


