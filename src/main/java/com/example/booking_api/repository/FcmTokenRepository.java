package com.example.booking_api.repository;

import com.example.booking_api.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FcmTokenRepository extends JpaRepository<FcmToken, UUID> {
    List<FcmToken> findByUser_Id(UUID userId);   // OK với @ManyToOne user
    Optional<FcmToken> findByToken(String token);
    boolean existsByToken(String token);

    // ⬇️ GIỮ lại nếu nơi khác có dùng
    void deleteByToken(String token);

    // ⬇️ THÊM: xóa token nhưng ràng buộc đúng chủ sở hữu
    void deleteByUser_IdAndToken(UUID userId, String token);

    // (tuỳ chọn) hỗ trợ kiểm tra tồn tại theo chủ sở hữu
    Optional<FcmToken> findByUser_IdAndToken(UUID userId, String token);
}
