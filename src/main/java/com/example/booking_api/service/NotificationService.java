package com.example.booking_api.service;

import com.example.booking_api.dto.NotificationResponse;
import com.example.booking_api.entity.Notification;
import com.example.booking_api.entity.User;
import com.example.booking_api.entity.enums.NotificationType;
import com.example.booking_api.repository.FcmTokenRepository;
import com.example.booking_api.repository.NotificationRepository;
import com.example.booking_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ⬇️ thêm vào để tạo + đẩy FCM
    private final UserRepository userRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmService fcmService;

    /** GET /api/v1/notifications?read=... */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsForUser(String firebaseUid, Boolean read) {
        List<Notification> notifications;

        if (read == null) {
            notifications = notificationRepository.findByFirebaseUid(firebaseUid);
        } else {
            notifications = notificationRepository.findByFirebaseUidAndRead(firebaseUid, read);
        }

        List<NotificationResponse> result = new ArrayList<>();
        for (Notification n : notifications) {
            result.add(NotificationResponse.fromEntity(n));
        }
        return result;
    }

    /** PUT /api/v1/notifications/{id}/read */
    @Transactional
    public NotificationResponse markAsRead(String firebaseUid, UUID notificationId) {
        Notification notification =
                notificationRepository.findOneByIdAndFirebaseUid(notificationId, firebaseUid);

        if (notification == null) {
            throw new IllegalArgumentException("Notification not found");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification);
    }

    // ================== CREATE + PUSH FCM ==================

    /**
     * Tạo thông báo cho chính user (theo firebaseUid), lưu DB và cố gắng đẩy FCM.
     * Trả về entity đã lưu (để tương thích chữ ký cũ của bạn).
     */
    @Transactional
    public Notification createNotification(
            String firebaseUid,
            NotificationType type,
            String title,
            String body
    ) {
        // 1) Lấy User từ firebaseUid
        User user = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // 2) Lưu thông báo vào DB
        Notification saved = notificationRepository.save(
                Notification.builder()
                        .userId(user.getId())   // users.id (UUID) map sang BINARY(16)
                        .type(type)
                        .title(title)
                        .body(body)
                        .read(false)
                        .build()
        );

        // 3) Đẩy FCM cho tất cả thiết bị của user này (không làm fail transaction nếu FCM lỗi)
        try {
            List<String> tokens = fcmTokenRepository.findByUser_Id(user.getId())
                    .stream()
                    .map(t -> t.getToken())
                    .toList();

            if (!tokens.isEmpty()) {
                fcmService.sendToTokens(tokens, title, body, Map.of(
                        "type", type.name(),
                        "notificationId", saved.getId().toString()
                ));
            }
        } catch (Exception ignore) {
            // Không để lỗi FCM làm rollback giao dịch lưu DB
        }

        return saved;
    }

    /**
     * Helper nếu bạn muốn trả thẳng DTO về Controller.
     */
    @Transactional
    public NotificationResponse createNotificationAndReturnDto(
            String firebaseUid,
            NotificationType type,
            String title,
            String body
    ) {
        return NotificationResponse.fromEntity(
                createNotification(firebaseUid, type, title, body)
        );
    }
}
