package com.example.booking_api.service;

import com.example.booking_api.dto.*;
import com.example.booking_api.entity.User;
import com.example.booking_api.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;

    private static final String FIREBASE_API_KEY = "AIzaSyCms5-dO8nbnmqBaK9GoplPTXUbMHnsKLc";

    public RegisterResponse register(RegisterRequest request) throws Exception {

        if (request.getEmail() == null || request.getPassword() == null || request.getFull_name() == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu bắt buộc");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email đã tồn tại");
        }

        UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                .setEmail(request.getEmail())
                .setPassword(request.getPassword())
                .setDisplayName(request.getFull_name());

        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            createRequest.setPhoneNumber("+84" + request.getPhone());
        }

        UserRecord userRecord = FirebaseAuth.getInstance().createUser(createRequest);

        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFull_name())
                .phone(request.getPhone())
                .firebaseUid(userRecord.getUid())
                .build();

        userRepository.save(user);

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

    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        try {
            // 1️⃣ Validate input
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("Email không được để trống");
            }

            String email = request.getEmail().trim();

            // 2️⃣ Kiểm tra email có tồn tại trong database không
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Email chưa được đăng ký"));

            // 3️⃣ Gọi Firebase REST API để GỬI EMAIL
            String firebaseUrl =
                    "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + FIREBASE_API_KEY;

            Map<String, Object> body = new HashMap<>();
            body.put("requestType", "PASSWORD_RESET");  // ✅ Loại request
            body.put("email", email);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();

            // ✅ Firebase sẽ TỰ ĐỘNG GỬI EMAIL
            ResponseEntity<Map> response = restTemplate.exchange(
                    firebaseUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                System.out.println("✅ Password reset email sent to: " + email);
                return new ForgotPasswordResponse("Email đặt lại mật khẩu đã được gửi thành công");
            } else {
                throw new RuntimeException("Firebase trả về lỗi: " + response.getStatusCode());
            }

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(e.getMessage());

        } catch (RuntimeException e) {
            if (e.getMessage().contains("chưa được đăng ký")) {
                throw e;
            }
            throw new RuntimeException("Lỗi khi gửi email reset password: " + e.getMessage());

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());

            // Xử lý lỗi Firebase cụ thể
            if (e.getMessage().contains("EMAIL_NOT_FOUND")) {
                throw new RuntimeException("Email chưa được đăng ký");
            }

            throw new RuntimeException("Lỗi gửi email: " + e.getMessage());
        }
    }
}
