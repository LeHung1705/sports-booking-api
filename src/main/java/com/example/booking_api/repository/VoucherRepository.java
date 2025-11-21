// src/main/java/com/example/booking_api/repository/VoucherRepository.java
package com.example.booking_api.repository;

import com.example.booking_api.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VoucherRepository extends JpaRepository<Voucher, UUID> {
    Optional<Voucher> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    // 👇 NEW: list voucher theo owner
    List<Voucher> findAllByOwner_Id(UUID ownerId);
}
