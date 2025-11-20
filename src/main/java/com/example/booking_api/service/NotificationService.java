package com.example.booking_api.service;

import com.example.booking_api.dto.NotificationResponse;
import com.example.booking_api.entity.Notification;
import com.example.booking_api.entity.enums.NotificationType;
import com.example.booking_api.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // firebaseUid chính là uid lấy từ token (FirebaseAuthFilter)
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

    // Đánh dấu 1 thông báo là đã đọc
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

    // Sau này gọi từ Booking/Payment để tạo thông báo mới
    // Sau này gọi từ Booking/Payment để tạo thông báo mới
    public Notification createNotification(
            String firebaseUid,
            NotificationType type,
            String title,
            String body
    ) {
        // TODO:
        // 1. Tìm User theo firebaseUid (UserRepository.findByFirebaseUid(firebaseUid))
        // 2. notification.setUserId(user.getId()); // UUID của user
        // 3. Lưu notification.
        // Hiện tại nhóm chưa dùng tới nên return null để tránh ghi sai user_id.
        return null;
    }
}
