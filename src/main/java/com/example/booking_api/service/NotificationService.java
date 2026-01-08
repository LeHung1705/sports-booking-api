package com.example.booking_api.service;

import com.example.booking_api.entity.FcmToken;
import com.example.booking_api.entity.Notification;
import com.example.booking_api.entity.User;
import com.example.booking_api.entity.enums.NotificationType;
import com.example.booking_api.repository.FcmTokenRepository;
import com.example.booking_api.repository.NotificationRepository;
import com.example.booking_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final FcmTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final ExpoPushService expoPushService;
    // 👇 BỔ SUNG: Inject thêm cái này
    private final NotificationRepository notificationRepository;
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
            // Truyền null vì hàm test này không có bookingId
            expoPushService.sendExpoNotification(t.getToken(), title, body, null);
        }
    }

    // Hàm cũ (Giữ lại để tương thích với BookingListener)
    @Transactional
    public void sendAndSaveNotification(User receiver, String title, String body, UUID bookingId, NotificationType type) {
        sendAndSaveNotification(receiver, title, body, bookingId, null, type);
    }

    // Hàm mới (Full options)
    @Transactional
    public void sendAndSaveNotification(User receiver, String title, String body, UUID bookingId, UUID venueId, NotificationType type) {
        // 1. LƯU VÀO DATABASE
        try {
            Notification noti = Notification.builder()
                    .userId(receiver.getId())
                    .title(title)
                    .body(body)
                    .bookingId(bookingId)
                    .venueId(venueId) // 👇 Lưu venueId
                    .type(type)
                    .read(false)
                    .createdAt(java.time.OffsetDateTime.now())
                    .build();

            notificationRepository.save(noti);
            System.out.println("💾 Đã lưu thông báo vào DB cho: " + receiver.getEmail());
        } catch (Exception e) {
            System.err.println("❌ Lỗi lưu DB: " + e.getMessage());
        }

        // 2. GỬI PUSH
        List<FcmToken> tokens = tokenRepository.findByUser(receiver);
        if (!tokens.isEmpty()) {
            Map<String, String> extraData = new HashMap<>();
            if (bookingId != null) extraData.put("bookingId", bookingId.toString());
            if (venueId != null) extraData.put("venueId", venueId.toString()); // 👇 Gửi kèm venueId
            extraData.put("type", type.name());

            for (FcmToken t : tokens) {
                expoPushService.sendExpoNotification(t.getToken(), title, body, extraData);
            }
        }
    }
    // 👇 BỔ SUNG HÀM LẤY DANH SÁCH (Cho Controller gọi)
    public List<Notification> getMyNotifications(String firebaseUid) {
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
    // Hàm xử lý đánh dấu 1 cái
    public void markAsRead(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        notification.setRead(true); // set is_read = true
        notificationRepository.save(notification);
    }

    // Hàm xử lý đánh dấu tất cả (Optional)
    public void markAllAsRead(UUID userId) {
        List<Notification> list = notificationRepository.findAllByUserId(userId);
        for (Notification n : list) {
            n.setRead(true);
        }
        notificationRepository.saveAll(list);
    }


}