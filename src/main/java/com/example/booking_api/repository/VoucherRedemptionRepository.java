package com.example.booking_api.repository;

import com.example.booking_api.entity.VoucherRedemption;
import com.example.booking_api.entity.Voucher;
import com.example.booking_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, UUID> {
    boolean existsByVoucherAndUser(Voucher voucher, User user);
}
