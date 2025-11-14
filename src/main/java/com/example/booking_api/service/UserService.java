package com.example.booking_api.service;

import com.example.booking_api.dto.user.UserProfileResponse;
import com.example.booking_api.dto.user.UserUpdateRequest;
import com.example.booking_api.entity.User;
import com.example.booking_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse getCurrentUser(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return new UserProfileResponse(
                user.getId().toString(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().toString()
        );
    }

    public UserProfileResponse updateProfile(String firebaseUid, UserUpdateRequest req) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (req.getFull_name() != null && !req.getFull_name().isEmpty())
            user.setFullName(req.getFull_name());
        if (req.getPhone() != null && !req.getPhone().isEmpty())
            user.setPhone(req.getPhone());

        userRepository.save(user);

        return new UserProfileResponse(
                user.getId().toString(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole().toString()
        );
    }
}
