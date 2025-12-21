package com.example.booking_api.repository;

import com.example.booking_api.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    // 1. 👇 BỔ SUNG HÀM NÀY ĐỂ FIX LỖI "Cannot resolve method" TRONG SERVICE
    List<Notification> findAllByUserId(UUID userId);

    // 2. Hàm lấy danh sách hiển thị (Sắp xếp mới nhất)
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // 3. Lấy theo firebase_uid (Sửa lại tên bảng/cột cho chắc chắn)
    @Query(value = """
            SELECT n.* FROM notifications n
                     JOIN users u ON n.user_id = u.id
            WHERE u.firebase_uid = :firebaseUid
            ORDER BY n.created_at DESC
            """, nativeQuery = true)
    List<Notification> findByFirebaseUid(@Param("firebaseUid") String firebaseUid);

    // 4. Lấy theo trạng thái đọc
    // ⚠️ LƯU Ý: Tôi đã sửa n.`read` thành n.is_read để khớp với Entity của bạn
    @Query(value = """
            SELECT n.* FROM notifications n
                     JOIN users u ON n.user_id = u.id
            WHERE u.firebase_uid = :firebaseUid
              AND n.is_read = :read  
            ORDER BY n.created_at DESC
            """, nativeQuery = true)
    List<Notification> findByFirebaseUidAndRead(@Param("firebaseUid") String firebaseUid,
                                                @Param("read") boolean read);

    // 5. Dùng cho việc check quyền sở hữu notification
    @Query(value = """
            SELECT n.* FROM notifications n
                     JOIN users u ON n.user_id = u.id
            WHERE n.id = :id
              AND u.firebase_uid = :firebaseUid
            """, nativeQuery = true)
    Optional<Notification> findOneByIdAndFirebaseUid(@Param("id") UUID id,
                                                     @Param("firebaseUid") String firebaseUid);
}