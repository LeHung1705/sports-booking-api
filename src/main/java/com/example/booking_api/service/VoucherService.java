package com.example.booking_api.service;

import com.example.booking_api.dto.voucher.*;
import com.example.booking_api.entity.Booking;
import com.example.booking_api.entity.User;
import com.example.booking_api.entity.Voucher;
import com.example.booking_api.entity.VoucherRedemption;
import com.example.booking_api.entity.enums.VoucherType;
import com.example.booking_api.repository.BookingRepository;
import com.example.booking_api.repository.UserRepository;
import com.example.booking_api.repository.VoucherRedemptionRepository;
import com.example.booking_api.repository.VoucherRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository redemptionRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;

    public VoucherService(VoucherRepository voucherRepository,
                          VoucherRedemptionRepository redemptionRepository,
                          BookingRepository bookingRepository,
                          UserRepository userRepository) {
        this.voucherRepository = voucherRepository;
        this.redemptionRepository = redemptionRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
    }

    /* ===== ADMIN ===== */

    @Transactional
    public Voucher create(VoucherRequest req) {
        if (req.getCode() == null || req.getCode().isBlank()) {
            throw new IllegalArgumentException("Code is required");
        }
        if (voucherRepository.existsByCode(req.getCode())) {
            throw new IllegalArgumentException("Code already exists");
        }
        Voucher v = new Voucher();
        v.setCode(req.getCode());
        v.setType(Optional.ofNullable(req.getType()).orElse(VoucherType.FIXED));
        v.setValue(Optional.ofNullable(req.getValue()).orElse(0.0));
        v.setMinOrderAmount(Optional.ofNullable(req.getMinOrderAmount()).orElse(0.0));
        v.setValidFrom(req.getValidFrom());
        v.setValidTo(req.getValidTo());
        v.setUsageLimit(req.getUsageLimit());
        v.setActive(Optional.ofNullable(req.getActive()).orElse(Boolean.TRUE));
        return voucherRepository.save(v);
    }

    @Transactional
    public Voucher update(UUID id, VoucherRequest req) {
        Voucher v = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));

        if (req.getCode() != null && !req.getCode().equals(v.getCode())) {
            if (voucherRepository.existsByCode(req.getCode())) {
                throw new IllegalArgumentException("Code already exists");
            }
            v.setCode(req.getCode());
        }
        if (req.getType() != null) v.setType(req.getType());
        if (req.getValue() != null) v.setValue(req.getValue());
        if (req.getMinOrderAmount() != null) v.setMinOrderAmount(req.getMinOrderAmount());
        if (req.getValidFrom() != null) v.setValidFrom(req.getValidFrom());
        if (req.getValidTo() != null) v.setValidTo(req.getValidTo());
        if (req.getUsageLimit() != null) v.setUsageLimit(req.getUsageLimit());
        if (req.getActive() != null) v.setActive(req.getActive());

        return voucherRepository.save(v);
    }

    public List<Voucher> list() {
        return voucherRepository.findAll();
    }

    @Transactional
    public void delete(UUID id) {
        voucherRepository.deleteById(id);
    }

    /* ===== USER: PREVIEW & REDEEM ===== */

    public PreviewResponse preview(PreviewRequest req) {
        double orderAmount = req.getOrderAmount() == null ? 0.0 : req.getOrderAmount();

        Voucher v = voucherRepository.findByCode(req.getCode()).orElse(null);
        if (v == null) return new PreviewResponse(false, 0, "Voucher not found");
        if (Boolean.FALSE.equals(v.getActive())) return new PreviewResponse(false, 0, "Voucher inactive");

        OffsetDateTime now = OffsetDateTime.now();
        if (v.getValidFrom() != null && now.isBefore(v.getValidFrom())) {
            return new PreviewResponse(false, 0, "Voucher not started");
        }
        if (v.getValidTo() != null && now.isAfter(v.getValidTo())) {
            return new PreviewResponse(false, 0, "Voucher expired");
        }
        if (v.getMinOrderAmount() != null && orderAmount < v.getMinOrderAmount()) {
            return new PreviewResponse(false, 0, "Order below minimum");
        }
        if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit()) {
            return new PreviewResponse(false, 0, "Usage limit reached");
        }

        double discount;
        if (v.getType() == VoucherType.PERCENT) {
            discount = orderAmount * (v.getValue() / 100.0);
        } else {
            discount = v.getValue();
        }
        if (discount < 0) discount = 0;
        if (discount > orderAmount) discount = orderAmount;

        return new PreviewResponse(true, discount, "OK");
    }

    @Transactional
    public RedeemResponse redeem(RedeemRequest req) {
        // 1) kiểm tra user & booking
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Booking booking = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // 2) voucher
        Voucher v = voucherRepository.findByCode(req.getCode())
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));

        // (tùy chọn) chặn 1 user dùng lại cùng code
        // if (redemptionRepository.existsByVoucherAndUser(v, user)) { throw new IllegalStateException("Already redeemed"); }

        // 3) lưu redemption
        VoucherRedemption r = new VoucherRedemption();
        r.setVoucher(v);
        r.setUser(user);
        r.setBooking(booking);
        r.setDiscountValue(req.getDiscountValue() == null ? 0.0 : req.getDiscountValue());
        redemptionRepository.save(r);

        // 4) tăng usedCount
        v.setUsedCount(v.getUsedCount() + 1);
        voucherRepository.save(v);

        return new RedeemResponse(r.getId(), v.getCode(), r.getDiscountValue());
    }
}
