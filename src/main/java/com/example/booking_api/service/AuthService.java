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

    public LoginResponse login (LoginRequest request) {
        try {
            if (request.getEmail() == null || request.getPassword() == null) {
                throw new IllegalArgumentException("Missing email or password");
            }

            String firebaseLoginUrl = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=[YOUR_FIREBASE_API_KEY]";

            Map<String, Object> body = new HashMap<>();
            body.put("email", request.getEmail());
            body.put("password", request.getPassword());
            body.put("returnSecureToken", true);

            // 🧠 Đơn giản hóa: mock login thành công (vì ta không gọi thật)
            Map<String, Object> user = new HashMap<>();
            user.put("email", request.getEmail());
            user.put("full_name", "Le Hung");

            String mockToken = "FAKE_TOKEN_FOR_TEST";

            return new LoginResponse(mockToken, user);

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("400: Missing data");
        }
    }
}
