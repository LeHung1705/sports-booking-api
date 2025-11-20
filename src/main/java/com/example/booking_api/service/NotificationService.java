package com.example.booking_api.service;

import com.example.booking_api.dto.NotificationResponse;
import com.example.booking_api.entity.Notification;
import com.example.booking_api.entity.enums.NotificationType;
import com.example.booking_api.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
            notifications = notificationRepository.findByFirebaseUidAndRead(firebaseUid, read.booleanValue());
        }

        List<NotificationResponse> result = new ArrayList<>();
        for (Notification n : notifications) {
            result.add(NotificationResponse.fromEntity(n));
        }
        return result;
    }

    // Nếu sau này tạo thông báo từ Booking/Payment thì cũng nên dùng firebaseUid
    public Notification createNotification(
            String firebaseUid,
            NotificationType type,
            String title,
            String body
    ) {
        // Lúc này bạn có thể:
        // 1. Tìm user theo firebase_uid (UserRepository.findByFirebaseUid)
        // 2. Gán vào notification trước khi save
        // (phần này có thể làm sau, giờ chỉ cần load list là được)
        return null;
    }
}
