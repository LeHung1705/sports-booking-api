package com.example.booking_api.service;

import com.example.booking_api.dto.user.UserListResponse;
import com.example.booking_api.entity.enums.UserRole;
import com.example.booking_api.repository.UserRepository;
import com.google.firebase.auth.ExportedUserRecord;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.ListUsersPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;

    public void updateUserRole(String firebaseUid, UserRole newRole) throws Exception {

        var user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setRole(newRole);
        userRepository.save(user);

        FirebaseAuth.getInstance()
                .setCustomUserClaims(firebaseUid, Map.of("role", newRole.name()));
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
