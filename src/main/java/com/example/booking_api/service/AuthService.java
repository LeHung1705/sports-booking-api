package com.example.booking_api.service;

import com.example.booking_api.dto.*;
import com.example.booking_api.entity.User;
import com.example.booking_api.repository.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException; // [FIX 4] Thêm import này
import com.google.firebase.auth.UserRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder; // [FIX 2] Thêm import này
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

    // [FIX 1] Bổ sung biến này để mã hóa/so sánh mật khẩu
    private final PasswordEncoder passwordEncoder;

    private static final String FIREBASE_API_KEY = "AIzaSyCms5-dO8nbnmqBaK9GoplPTXUbMHnsKLc";

    public RegisterResponse register(RegisterRequest request) throws Exception {

        if (request.getEmail() == null || request.getPassword() == null || request.getFull_name() == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu bắt buộc");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email đã tồn tại");
        }

        // 1️⃣ Tạo user trong Firebase
        UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
                .setEmail(request.getEmail())
                .setPassword(request.getPassword())
                .setDisplayName(request.getFull_name());

        if (request.getPhone() != null && !request.getPhone().isEmpty()) {
            createRequest.setPhoneNumber("+84" + request.getPhone());
        }

        UserRecord firebaseUser = FirebaseAuth.getInstance().createUser(createRequest);

        // 2️⃣ Xác định role của user này
        boolean adminExists = userRepository.existsByRole(com.example.booking_api.entity.enums.UserRole.ADMIN);

        com.example.booking_api.entity.enums.UserRole assignedRole =
                adminExists ? com.example.booking_api.entity.enums.UserRole.USER
                        : com.example.booking_api.entity.enums.UserRole.ADMIN;

        // 3️⃣ Tạo user trong MySQL
        User user = User.builder()
                .email(request.getEmail())
                .fullName(request.getFull_name())
                .phone(request.getPhone())
                .firebaseUid(firebaseUser.getUid())
                .role(assignedRole)
                // [FIX 6] QUAN TRỌNG: Phải lưu pass vào MySQL thì mới đổi pass được sau này
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        userRepository.save(user);

        // 4️⃣ Cập nhật ROLE vào Firebase Custom Claims
        FirebaseAuth.getInstance().setCustomUserClaims(
                firebaseUser.getUid(),
                Map.of("role", assignedRole.name())
        );

        // 5️⃣ Trả custom token
        String customToken = FirebaseAuth.getInstance().createCustomToken(firebaseUser.getUid());

        return new RegisterResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getFullName(),
                customToken
        );
    }

    public LoginResponse login(LoginRequest request) {
        try {
            if (request.getEmail() == null || request.getPassword() == null) {
                throw new IllegalArgumentException("Missing email or password");
            }

            String firebaseLoginUrl =
                    "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + FIREBASE_API_KEY;

            Map<String, Object> body = new HashMap<>();
            body.put("email", request.getEmail());
            body.put("password", request.getPassword());
            body.put("returnSecureToken", true);

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.postForObject(firebaseLoginUrl, body, Map.class);

            String idToken = (String) response.get("idToken");
            // String localId = (String) response.get("localId"); // Không dùng biến này thì comment lại cho đỡ warning

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("email", user.getEmail());
            userInfo.put("full_name", user.getFullName());
            userInfo.put("firebaseUid", user.getFirebaseUid());

            // Nên trả về role để FE biết đường phân quyền
            userInfo.put("role", user.getRole());

            return new LoginResponse(idToken, userInfo);

        } catch (Exception e) {
            throw new RuntimeException("403: Login failed - " + e.getMessage());
        }
    }

    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
        try {
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                throw new IllegalArgumentException("Email không được để trống");
            }

            String email = request.getEmail().trim();

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("Email chưa được đăng ký"));

            String firebaseUrl =
                    "https://identitytoolkit.googleapis.com/v1/accounts:sendOobCode?key=" + FIREBASE_API_KEY;

            Map<String, Object> body = new HashMap<>();
            body.put("requestType", "PASSWORD_RESET");
            body.put("email", email);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            RestTemplate restTemplate = new RestTemplate();

            ResponseEntity<Map> response = restTemplate.exchange(
                    firebaseUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
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
            if (e.getMessage().contains("EMAIL_NOT_FOUND")) {
                throw new RuntimeException("Email chưa được đăng ký");
            }
            throw new RuntimeException("Lỗi gửi email: " + e.getMessage());
        }
    }

    public LogoutResponse logout() {
        System.out.println("User logged out");
        return new LogoutResponse("Logged out successfully");
    }

    // --- HÀM BỔ SUNG MỚI (Đã sửa import và logic) ---
    public void changePassword(ChangePasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        // [SỬA DÒNG NÀY] dùng getPasswordHash() thay vì getPassword()
        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu hiện tại không đúng");
        }

        // [SỬA DÒNG NÀY] dùng setPasswordHash() thay vì setPassword()
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));

        userRepository.save(user);

        // 4. CẬP NHẬT PASS TRÊN FIREBASE
        try {
            UserRecord.UpdateRequest updateRequest = new UserRecord.UpdateRequest(user.getFirebaseUid())
                    .setPassword(request.getNewPassword());
            FirebaseAuth.getInstance().updateUser(updateRequest);
        } catch (FirebaseAuthException e) { // [FIX 4] Đã import FirebaseAuthException
            throw new RuntimeException("Lỗi cập nhật mật khẩu trên Firebase: " + e.getMessage());
        }
    }
}