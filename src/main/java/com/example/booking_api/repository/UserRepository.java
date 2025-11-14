package com.example.booking_api.repository;

import com.example.booking_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // 🔹 Kiểm tra email đã tồn tại (dùng trong register)
    boolean existsByEmail(String email);

    // 🔹 Tìm user theo email (nếu cần login)
    Optional<User> findByEmail(String email);

    // 🔹 Tìm user theo Firebase UID (dùng cho AuthGuard và UserService)
    Optional<User> findByFirebaseUid(String firebaseUid);
}
