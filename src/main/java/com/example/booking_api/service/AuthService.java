package com.example.booking_api.service;

import com.example.booking_api.dto.LoginRequest;
import com.example.booking_api.dto.LoginResponse;
import com.example.booking_api.dto.RegisterRequest;
import com.example.booking_api.dto.RegisterResponse;
import com.example.booking_api.entity.User;
import com.example.booking_api.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    public RegisterResponse register(RegisterRequest request) throws Exception {

        // 1️⃣ Kiểm tra input
        if (request.getEmail() == null || request.getPassword() == null || request.getFull_name() == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu bắt buộc");
        }

        // 2️⃣ Kiểm tra email tồn tại
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email đã tồn tại");
        }

        // 3️⃣ Tạo user trong Firebase
        UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                .setEmail(request.getEmail())
                .setPassword(request.getPassword())
                .setDisplayName(request.getFull_name());

        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            createRequest.setPhoneNumber("+84" + request.getPhone());
        }

        UserRecord userRecord = FirebaseAuth.getInstance().createUser(createRequest);

        // 4️⃣ Lưu vào DB
        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFull_name())
                .phone(request.getPhone())
                .firebaseUid(userRecord.getUid())
                .build();

        userRepository.save(user);

        // 5️⃣ Tạo custom token
        String token = FirebaseAuth.getInstance().createCustomToken(userRecord.getUid());

        return new RegisterResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFullName(),
                token
        );
    }

    public LoginResponse login(LoginRequest request) {
        try {
            if (request.getEmail() == null || request.getPassword() == null) {
                throw new IllegalArgumentException("Missing email or password");
            }

            String firebaseLoginUrl =
                    "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=AIzaSyCms5-dO8nbnmqBaK9GoplPTXUbMHnsKLc";

            Map<String, Object> body = new HashMap<>();
            body.put("email", request.getEmail());
            body.put("password", request.getPassword());
            body.put("returnSecureToken", true);

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.postForObject(firebaseLoginUrl, body, Map.class);

            String idToken = (String) response.get("idToken");
            String localId = (String) response.get("localId");

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("email", user.getEmail());
            userInfo.put("full_name", user.getFullName());
            userInfo.put("firebaseUid", user.getFirebaseUid());

            return new LoginResponse(idToken, userInfo);

        } catch (Exception e) {
            throw new RuntimeException("403: Login failed - " + e.getMessage());
        }
    }


}
