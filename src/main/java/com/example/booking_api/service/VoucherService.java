// src/main/java/com/example/booking_api/service/VoucherService.java
package com.example.booking_api.service;

import com.example.booking_api.dto.voucher.*;
import com.example.booking_api.entity.*;
import com.example.booking_api.entity.enums.VoucherType;
import com.example.booking_api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final VenueRepository venueRepository; // 👈 NEW

    public VoucherService(VoucherRepository voucherRepository,
                          VoucherRedemptionRepository redemptionRepository,
                          BookingRepository bookingRepository,
                          UserRepository userRepository,
                          VenueRepository venueRepository) {
        this.voucherRepository = voucherRepository;
        this.redemptionRepository = redemptionRepository;
        this.bookingRepository = bookingRepository;
        this.userRepository = userRepository;
        this.venueRepository = venueRepository; // 👈 NEW
    }

    /* ===== OWNER CRUD ===== */

    @Transactional
    public Voucher createForOwner(String firebaseUid, VoucherRequest req) {
        if (req.getCode() == null || req.getCode().isBlank()) {
            throw new IllegalArgumentException("Code is required");
        }
        if (voucherRepository.existsByCodeIgnoreCase(req.getCode())) {
            throw new IllegalArgumentException("Code already exists");
        }

        User owner = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));

        Voucher v = new Voucher();
        v.setOwner(owner); // 👈 quan trọng

        v.setCode(req.getCode());
        v.setType(Optional.ofNullable(req.getType()).orElse(VoucherType.FIXED));
        v.setValue(Optional.ofNullable(req.getValue()).orElse(BigDecimal.ZERO));
        v.setMinOrderAmount(Optional.ofNullable(req.getMinOrderAmount()).orElse(BigDecimal.ZERO));
        v.setValidFrom(req.getValidFrom());
        v.setValidTo(req.getValidTo());
        v.setUsageLimit(req.getUsageLimit());
        v.setActive(Optional.ofNullable(req.getActive()).orElse(Boolean.TRUE));
        return voucherRepository.save(v);
    }

    @Transactional
    public Voucher updateOwned(String firebaseUid, UUID id, VoucherRequest req) {
        User owner = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));

        Voucher v = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));

        if (!v.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You do not own this voucher");
        }

        if (req.getCode() != null && !req.getCode().equalsIgnoreCase(v.getCode())) {
            if (voucherRepository.existsByCodeIgnoreCase(req.getCode())) {
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

    public List<Voucher> listByOwner(String firebaseUid) {
        User owner = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));
        return voucherRepository.findAllByOwner_Id(owner.getId());
    }

    @Transactional
    public void deleteOwned(String firebaseUid, UUID id) {
        User owner = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));

        Voucher v = voucherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));

        if (!v.getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException("You do not own this voucher");
        }
        voucherRepository.delete(v);
    }

    /* ===== USER: PREVIEW & REDEEM ===== */

    public PreviewResponse preview(PreviewRequest req) {
        BigDecimal orderAmount = req.getOrderAmount() == null ? BigDecimal.ZERO : req.getOrderAmount();

        Voucher v = voucherRepository.findByCodeIgnoreCase(req.getCode()).orElse(null);
        if (v == null) return new PreviewResponse(false, BigDecimal.ZERO, "Voucher not found");
        if (Boolean.FALSE.equals(v.getActive())) return new PreviewResponse(false, BigDecimal.ZERO, "Voucher inactive");

        OffsetDateTime now = OffsetDateTime.now();
        if (v.getValidFrom() != null && now.isBefore(v.getValidFrom())) {
            return new PreviewResponse(false, BigDecimal.ZERO, "Voucher not started");
        }
        if (v.getValidTo() != null && now.isAfter(v.getValidTo())) {
            return new PreviewResponse(false, BigDecimal.ZERO, "Voucher expired");
        }
        if (v.getMinOrderAmount() != null && orderAmount.compareTo(v.getMinOrderAmount()) < 0) {
            return new PreviewResponse(false, BigDecimal.ZERO, "Order below minimum");
        }
        if (v.getUsageLimit() != null && v.getUsedCount() >= v.getUsageLimit()) {
            return new PreviewResponse(false, BigDecimal.ZERO, "Usage limit reached");
        }

        // 👇 NEW: bắt buộc có venueId để check owner
        if (req.getVenueId() == null) {
            return new PreviewResponse(false, BigDecimal.ZERO, "Voucher không hợp lệ");
        }
        Venue venue = venueRepository.findById(req.getVenueId())
                .orElse(null);
        if (venue == null) {
            return new PreviewResponse(false, BigDecimal.ZERO, "Voucher không hợp lệ");
        }
        
        // Fix: Allow Admin voucher (owner == null) OR Owner Match
        if (v.getOwner() != null) {
             if (venue.getOwner() == null || !venue.getOwner().getId().equals(v.getOwner().getId())) {
                 return new PreviewResponse(false, BigDecimal.ZERO, "Voucher not applicable to this venue");
             }
        }

        BigDecimal discount;
        if (v.getType() == VoucherType.PERCENT) {
            discount = orderAmount
                    .multiply(v.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            discount = v.getValue();
        }

        if (discount.compareTo(BigDecimal.ZERO) < 0) discount = BigDecimal.ZERO;
        if (discount.compareTo(orderAmount) > 0) discount = orderAmount;

        return new PreviewResponse(true, discount, "OK");
    }

    @Transactional
    public RedeemResponse redeem(RedeemRequest req) {
        // 1) user & booking
        User user = userRepository.findById(req.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        Booking booking = bookingRepository.findById(req.getBookingId())
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        // 2) voucher
        Voucher v = voucherRepository.findByCodeIgnoreCase(req.getCode())
                .orElseThrow(() -> new IllegalArgumentException("Voucher not found"));

        // 👇 NEW: check owner(voucher) == owner(booking.venue)
        if (booking.getCourt() == null || booking.getCourt().getVenue() == null || booking.getCourt().getVenue().getOwner() == null) {
            throw new IllegalArgumentException("Booking missing venue/owner");
        }
        UUID venueOwnerId = booking.getCourt().getVenue().getOwner().getId();
        if (!v.getOwner().getId().equals(venueOwnerId)) {
            throw new IllegalArgumentException("Voucher not applicable to this venue");
        }

        // 3) lưu redemption
        VoucherRedemption r = new VoucherRedemption();
        r.setVoucher(v);
        r.setUser(user);
        r.setBooking(booking);
        r.setDiscountValue(req.getDiscountValue() == null ? BigDecimal.ZERO : req.getDiscountValue());
        redemptionRepository.save(r);

        // 4) tăng usedCount
        v.setUsedCount(v.getUsedCount() + 1);
        voucherRepository.save(v);

        return new RedeemResponse(r.getId(), v.getCode(), r.getDiscountValue());
    }
}
