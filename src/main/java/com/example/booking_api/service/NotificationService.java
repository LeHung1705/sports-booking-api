package com.example.booking_api.service;

import com.example.booking_api.entity.FcmToken;
import com.example.booking_api.entity.User;
import com.example.booking_api.repository.FcmTokenRepository;
import com.example.booking_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final FcmTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ExpoPushService expoPushService;

    // --- 1. Đăng ký Token ---
    @Transactional
    public void registerToken(String firebaseUid, String token, String deviceType) {
        if (token == null || token.isBlank()) return;

        // ✅ ĐÚNG: Tìm user bằng Firebase UID
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found with UID: " + firebaseUid));

        tokenRepository.findByToken(token).ifPresentOrElse(
                existed -> {
                    if (!Objects.equals(existed.getUser().getId(), user.getId())) {
                        existed.setUser(user);
                    }
                    if (deviceType != null) existed.setDevice(deviceType);
                    tokenRepository.save(existed);
                },
                () -> tokenRepository.save(
                        FcmToken.builder()
                                .user(user)
                                .token(token)
                                .device(deviceType) // "ios" hoặc "android"
                                .build()
                )
        );
        System.out.println("✅ (Register) Đã lưu token cho user: " + user.getEmail());
    }

    // --- 2. Hủy Token ---
    @Transactional
    public void unregisterToken(String firebaseUid, String token) {
        // ✅ ĐÚNG: Tìm user bằng Firebase UID
        User user = userRepository.findByFirebaseUid(firebaseUid).orElse(null);
        if (user == null) return;

        tokenRepository.findByUserAndToken(user, token)
                .ifPresent(tokenRepository::delete);

        System.out.println("✅ (Unregister) Đã hủy token của user: " + user.getEmail());
    }

    // --- 3. Gửi thông báo (Hàm bạn đang bị lỗi ở đây) ---
    public void sendNotificationToUser(String firebaseUid, String title, String body) {
        System.out.println("🔍 Đang tìm User với UID: " + firebaseUid);

        // ⚠️ SỬA LẠI CHỖ NÀY QUAN TRỌNG NHẤT:
        // Cũ (Sai): findByEmail(firebaseUid) -> Log báo tìm email=? là sai.
        // Mới (Đúng): findByFirebaseUid(firebaseUid)
        User user = userRepository.findByFirebaseUid(firebaseUid).orElse(null);

        if (user == null) {
            System.err.println("❌ LỖI: Không tìm thấy User nào có UID là " + firebaseUid);
            return;
        }

        List<FcmToken> tokens = tokenRepository.findByUser(user);

        if (tokens.isEmpty()) {
            System.err.println("⚠️ User " + user.getEmail() + " có tồn tại nhưng KHÔNG CÓ Token nào trong bảng fcm_tokens!");
            return;
        }

        // Gửi cho tất cả token của user đó
        for (FcmToken t : tokens) {
            System.out.println("🚀 Đang bắn thông báo tới Token: " + t.getToken());
            expoPushService.sendExpoNotification(t.getToken(), title, body);
        }
    }
}