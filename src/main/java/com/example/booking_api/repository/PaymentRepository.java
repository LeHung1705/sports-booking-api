package com.example.booking_api.repository;

import com.example.booking_api.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);
}
