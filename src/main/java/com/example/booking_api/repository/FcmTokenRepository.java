package com.example.booking_api.repository;

import com.example.booking_api.entity.FcmToken;
import com.example.booking_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface FcmTokenRepository extends JpaRepository<FcmToken, UUID> {
    // Tìm token xem đã tồn tại chưa (bất kể của user nào)
    Optional<FcmToken> findByToken(String token);

    // Tìm danh sách token của 1 user (để gửi thông báo)
    List<FcmToken> findByUser(User user);

    // Tìm token cụ thể của user (để xóa khi logout)
    // Sửa lỗi: dùng findByUserAndToken thay vì findByUser_IdAndToken cho dễ hiểu
    Optional<FcmToken> findByUserAndToken(User user, String token);

    void deleteByToken(String token);
}