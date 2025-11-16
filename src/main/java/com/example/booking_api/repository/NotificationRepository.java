package com.example.booking_api.repository;

import com.example.booking_api.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // Lấy tất cả thông báo theo firebase_uid
    @Query(value = """
            SELECT n.* 
            FROM notifications n
                     JOIN users u ON n.user_id = u.id
            WHERE u.firebase_uid = :firebaseUid
            ORDER BY n.created_at DESC
            """, nativeQuery = true)
    List<Notification> findByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    // Lấy theo firebase_uid + trạng thái đọc
    @Query(value = """
            SELECT n.* 
            FROM notifications n
                     JOIN users u ON n.user_id = u.id
            WHERE u.firebase_uid = :firebaseUid
              AND n.`read` = :read
            ORDER BY n.created_at DESC
            """, nativeQuery = true)
    List<Notification> findByFirebaseUidAndRead(@Param("firebaseUid") String firebaseUid,
                                                @Param("read") boolean read);
}
