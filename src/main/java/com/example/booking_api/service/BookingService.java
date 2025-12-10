package com.example.booking_api.service;

import com.example.booking_api.dto.booking.*;
import com.example.booking_api.entity.*;
import com.example.booking_api.entity.enums.BookingStatus;
import com.example.booking_api.entity.enums.PaymentStatus;
import com.example.booking_api.entity.enums.VoucherType;
import com.example.booking_api.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;

    public List<BookingListResponse> listUserBookings(String firebaseUid, BookingListRequest req) {
        try {
            User user = userRepository.findByFirebaseUid(firebaseUid)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<Booking> bookings = bookingRepository.findByUserWithFilter(
                    user.getId(),
                    req.getStatus(),
                    req.getFrom(),
                    req.getTo()
            );

            return bookings.stream()
                    .map(b -> BookingListResponse.builder()
                            .id(b.getId())
                            .venue(b.getCourt().getVenue().getName())
                            .court(b.getCourt().getName())
                            .startTime(b.getStartTime())
                            .endTime(b.getEndTime())
                            .totalPrice(b.getTotalAmount())
                            .status(b.getStatus() == null ? null : b.getStatus().name())
                            .build())
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("QUERY_ERROR", e);
        }
    }

    @Transactional
    public BookingCreateResponse createBooking(String firebaseUid, BookingCreateRequest req) {
        System.out.println("DEBUG SERVICE - Searching for firebaseUid: " + firebaseUid);
        User user = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));

        LocalDateTime start = req.getStartTime();
        LocalDateTime end = req.getEndTime();

        if (start == null || end == null) {
            throw new IllegalArgumentException("start_time and end_time are required");
        }
        if (!start.isBefore(end)) {
            throw new IllegalArgumentException("start_time must be before end_time");
        }

        Court court = courtRepository.findById(req.getCourtId()).orElseThrow(() -> new RuntimeException("Court not found"));

        if (Boolean.FALSE.equals(court.getIsActive())) {
            throw new IllegalArgumentException("Court is not active");
        }

        long overlapCount = bookingRepository.countOverlappingBookings(court.getId(), start, end);

        if (overlapCount > 0) {
            throw new RuntimeException("TIME_OVERLAP");
        }

        Duration duration = Duration.between(start, end);
        long minutes = duration.toMinutes();
        if (minutes < 0) {
            throw new IllegalArgumentException("duration must be greater than zero");
        }

        // Logic tính giá Dynamic
        BigDecimal totalAmount = BigDecimal.ZERO;
        Venue venue = court.getVenue();

        LocalDateTime slotStart = start;
        while (slotStart.isBefore(end)) {
            LocalDateTime slotEnd = slotStart.plusMinutes(30);
            if (slotEnd.isAfter(end)) {
                slotEnd = end;
            }

            LocalTime slotStartTimeLocal = slotStart.toLocalTime();
            BigDecimal slotBasePricePerHour = court.getPricePerHour();

            if (venue.getPricingConfig() != null) {
                for (PricingRule rule : venue.getPricingConfig()) {
                    if (!slotStartTimeLocal.isBefore(rule.getStartTime()) && slotStartTimeLocal.isBefore(rule.getEndTime())) {
                        slotBasePricePerHour = rule.getPricePerHour();
                        break;
                    }
                }
            }

            BigDecimal slotPrice = slotBasePricePerHour.multiply(new BigDecimal("0.5"));
            totalAmount = totalAmount.add(slotPrice);

            slotStart = slotEnd;
        }

        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);

        BigDecimal amountToPay;
        BigDecimal depositAmount;

        if ("DEPOSIT".equalsIgnoreCase(req.getPaymentOption())) {
             // 30% deposit
            depositAmount = totalAmount.multiply(new BigDecimal("0.3")).setScale(2, RoundingMode.HALF_UP);
            amountToPay = depositAmount;
        } else {
            // Full payment
            depositAmount = totalAmount; // For full payment, deposit is essentially the full amount
            amountToPay = totalAmount;
        }


        LocalDateTime now = LocalDateTime.now();

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCourt(court);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setTotalAmount(totalAmount);
        booking.setDepositAmount(depositAmount);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setStatus(BookingStatus.PENDING_PAYMENT); // Set initial status to PENDING_PAYMENT
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);

        Booking saved = bookingRepository.save(booking);
        return BookingCreateResponse.builder()
                .id(saved.getId())
                .totalAmount(saved.getTotalAmount())
                .amountToPay(amountToPay)
                .depositAmount(saved.getDepositAmount())
                .status(saved.getStatus())
                .bankBin(venue.getBankBin())
                .bankAccountNumber(venue.getBankAccountNumber())
                .bankAccountName(venue.getBankAccountName())
                .build();
    }

    public List<TimeSlotResponse> getAvailability(UUID courtId, LocalDate date) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new RuntimeException("Court not found"));
        Venue venue = court.getVenue();

        // 1. Xác định khoảng thời gian tìm kiếm (Search Window)
        LocalDateTime searchStart = date.atStartOfDay();
        LocalDateTime searchEnd = date.plusDays(1).atStartOfDay();

        List<Booking> bookings = bookingRepository.findByCourtAndDateRange(courtId, searchStart, searchEnd);

        List<TimeSlotResponse> slots = new ArrayList<>();

        // Loop từ 05:00 -> 23:00
        LocalTime current = LocalTime.of(5, 0);
        LocalTime closeTime = LocalTime.of(23, 0);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        
        // Capture current time to disable past slots
        LocalDateTime now = LocalDateTime.now();

        while (current.isBefore(closeTime)) {
            // Force 30-Minute Interval Loop
            LocalTime next = current.plusMinutes(30);

            // Tạo Slot chuẩn
            LocalDateTime slotStart = LocalDateTime.of(date, current);
            LocalDateTime slotEnd = LocalDateTime.of(date, next);
            
            // Check if slot is in the past
            boolean isPast = slotStart.isBefore(now);

            // 2. Check Overlap
            boolean isBooked = bookings.stream().anyMatch(b -> {
                // Bỏ qua đơn đã huỷ hoặc lỗi
                if (b.getStatus() == BookingStatus.CANCELED || b.getStatus() == BookingStatus.FAILED) {
                    return false;
                }

                LocalDateTime bStart = b.getStartTime();
                LocalDateTime bEnd = b.getEndTime();

                // Logic Giao nhau: (StartA < EndB) VÀ (EndA > StartB)
                return slotStart.isBefore(bEnd) && slotEnd.isAfter(bStart);
            });

            // 3. Tính giá (Dynamic Pricing)
            BigDecimal pricePerHour = court.getPricePerHour();
            if (venue.getPricingConfig() != null) {
                for (PricingRule rule : venue.getPricingConfig()) {
                    if (!current.isBefore(rule.getStartTime()) && !next.isAfter(rule.getEndTime())) {
                        pricePerHour = rule.getPricePerHour();
                        break;
                    }
                }
            }
            BigDecimal slotPrice = pricePerHour.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);

            slots.add(TimeSlotResponse.builder()
                    .time(current.format(formatter))     // Trả về string "07:00"
                    .endTime(next.format(formatter))     // Trả về string "07:30"
                    .price(slotPrice)
                    .status((isBooked || isPast) ? "booked" : "available")
                    .build());

            current = next;
        }
        return slots;
    }

    public BookingDetailResponse getBookingDetail(String firebaseUid, UUID bookingId) {
        User me = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Booking b = bookingRepository.findDetailById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        boolean isUser = b.getUser() != null && b.getUser().getId().equals(me.getId());
        boolean isOwner = b.getCourt() != null
                && b.getCourt().getVenue() != null
                && b.getCourt().getVenue().getOwner() != null
                && b.getCourt().getVenue().getOwner().getId().equals(me.getId());
        if (!isUser && !isOwner) {
            throw new SecurityException("FORBIDDEN");
        }

        Payment payment = b.getPayment();

        BookingDetailResponse.PaymentItem paymentDto = null;
        if (payment != null) {
            paymentDto = BookingDetailResponse.PaymentItem.builder()
                    .id(payment.getId())
                    .amount(payment.getAmount())
                    .status(payment.getStatus() == null ? null : payment.getStatus().name())
                    .returnPayload(payment.getReturnPayload())
                    .build();
        }

        return BookingDetailResponse.builder()
                .id(b.getId())
                .venue(b.getCourt().getVenue().getName())
                .court(b.getCourt() == null ? null : b.getCourt().getName())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .totalPrice(b.getTotalAmount())
                .status(b.getStatus() == null ? null : b.getStatus().name())
                .payment(paymentDto)
                .build();
    }

    @Transactional
    public BookingApplyVoucherResponse applyVoucher(String firebaseUid, UUID bookingId, BookingApplyVoucherRequest req) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(me.getId())) {
            throw new SecurityException("FORBIDDEN");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("BOOKING_STATUS_NOT_ALLOWED");
        }

        if (voucherRedemptionRepository.existsByBooking_Id(booking.getId())) {
            throw new RuntimeException("VOUCHER_ALREADY_APPLIED");
        }

        BigDecimal originalPrice = booking.getTotalAmount();
        if (originalPrice == null) {
            throw new RuntimeException("BOOKING_PRICE_NOT_SET");
        }

        System.out.println("DEBUG APPLY VOUCHER:");
        System.out.println("  -> Booking ID: " + booking.getId());
        System.out.println("  -> Original Price (Total Amount): " + originalPrice);

        String code = req.getVoucherCode().trim();
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new RuntimeException("VOUCHER_NOT_FOUND"));

        System.out.println("  -> Voucher Code: " + voucher.getCode());
        System.out.println("  -> Voucher Min Order: " + voucher.getMinOrderAmount());

        // Validate voucher
        LocalDateTime now = LocalDateTime.now();
        
        // Convert Voucher OffsetDateTime to LocalDateTime for simple comparison
        LocalDateTime voucherValidFrom = voucher.getValidFrom() != null ? voucher.getValidFrom().toLocalDateTime() : null;
        LocalDateTime voucherValidTo = voucher.getValidTo() != null ? voucher.getValidTo().toLocalDateTime() : null;

        if (Boolean.FALSE.equals(voucher.getActive())) {
            throw new RuntimeException("VOUCHER_EXPIRED");
        }

        if (voucherValidFrom != null && now.isBefore(voucherValidFrom)) {
            throw new RuntimeException("VOUCHER_EXPIRED");
        }

        if (voucherValidTo != null && now.isAfter(voucherValidTo)) {
            throw new RuntimeException("VOUCHER_EXPIRED");
        }

        if (voucher.getUsageLimit() != null
                && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new RuntimeException("VOUCHER_EXPIRED");
        }

        if (voucher.getMinOrderAmount() != null
                && originalPrice.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new RuntimeException("MIN_ORDER_NOT_REACHED: Booking Total (" + originalPrice + ") < Voucher Min (" + voucher.getMinOrderAmount() + ")");
        }

        // Check if voucher is applicable to this venue
        if (voucher.getOwner() != null) {
            UUID venueOwnerId = booking.getCourt().getVenue().getOwner().getId();
            if (!venueOwnerId.equals(voucher.getOwner().getId())) {
                throw new RuntimeException("VOUCHER_NOT_APPLICABLE");
            }
        }


        // Calc discount
        BigDecimal discount = BigDecimal.ZERO;

        if (voucher.getType() == VoucherType.FIXED) {
            discount = voucher.getValue();
        } else if (voucher.getType() == VoucherType.PERCENT) {
            discount = originalPrice
                    .multiply(voucher.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }
        if (discount.compareTo(originalPrice) > 0) {
            discount = originalPrice;
        }

        BigDecimal newTotal = originalPrice.subtract(discount).setScale(2, RoundingMode.HALF_UP);

        // Update booking
        booking.setDiscountAmount(discount);
        booking.setTotalAmount(newTotal);
        booking.setUpdatedAt(now);

        voucher.setUsedCount(voucher.getUsedCount() + 1);

        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setVoucher(voucher);
        redemption.setUser(me);
        redemption.setBooking(booking);
        redemption.setDiscountValue(discount);
        // Assuming VoucherRedemption still uses OffsetDateTime
        redemption.setCreatedAt(java.time.OffsetDateTime.now());

        voucherRepository.save(voucher);
        bookingRepository.save(booking);
        voucherRedemptionRepository.save(redemption);

        return BookingApplyVoucherResponse.builder()
                .id(booking.getId())
                .originalPrice(originalPrice)
                .discountValue(discount)
                .totalAmount(newTotal)
                .voucherCode(voucher.getCode())
                .build();
    }

    @Transactional
    public BookingApplyVoucherResponse removeVoucher(String firebaseUid, UUID bookingId) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(me.getId())) {
            throw new SecurityException("FORBIDDEN");
        }

        VoucherRedemption redemption = voucherRedemptionRepository.findByBooking_Id(bookingId).orElseThrow(() -> new RuntimeException("NO_VOUCHER_APPLIED"));

        BigDecimal original = booking.getTotalAmount().add(redemption.getDiscountValue());
        booking.setTotalAmount(original);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setUpdatedAt(LocalDateTime.now());


        Voucher voucher = redemption.getVoucher();
        voucher.setUsedCount(Math.max(0, voucher.getUsedCount() - 1));

        voucherRedemptionRepository.delete(redemption);

        voucherRepository.save(voucher);
        bookingRepository.save(booking);

        return BookingApplyVoucherResponse.builder()
                .id(booking.getId())
                .originalPrice(original)
                .discountValue(BigDecimal.ZERO)
                .totalAmount(original)
                .voucherCode(null)
                .build();
    }

    @Transactional
    public BookingCancelResponse cancelBooking(String firebaseUid, UUID bookingId, BookingCancelRequest req) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        boolean isUser = booking.getUser().getId().equals(me.getId());
        boolean isOwner = booking.getCourt().getVenue().getOwner().getId().equals(me.getId());

        if (!isUser && !isOwner) {
            throw new SecurityException("FORBIDDEN");
        }

        if (booking.getStatus() == BookingStatus.CANCELED) {
            throw new RuntimeException("ALREADY_CANCELED");
        }
        if (booking.getStatus() == BookingStatus.COMPLETED) {
            throw new RuntimeException("CANCEL_WINDOW_PASSED");
        }

        LocalDateTime now = LocalDateTime.now();
        if (booking.getStartTime() != null && !now.isBefore(booking.getStartTime())) {
            throw new RuntimeException("Cannot cancel past booking");
        }

        // Calculate refund based on policy
        // 1. > 6 hours: 100%
        // 2. 2 - 6 hours: 50%
        // 3. < 2 hours: 0%
        long minutesUntilStart = Duration.between(now, booking.getStartTime()).toMinutes();
        BigDecimal refundPercentage;

        if (minutesUntilStart > 360) { // > 6 hours
            refundPercentage = BigDecimal.ONE;
        } else if (minutesUntilStart >= 120) { // 2 <= hours <= 6
            refundPercentage = new BigDecimal("0.5");
        } else { // < 2 hours
            refundPercentage = BigDecimal.ZERO;
        }

        // Deposit is 30% of total amount
        BigDecimal depositAmount = booking.getTotalAmount().multiply(new BigDecimal("0.3"));
        BigDecimal refundAmount = depositAmount.multiply(refundPercentage).setScale(2, RoundingMode.HALF_UP);

        Payment payment = booking.getPayment();
        if (payment != null) {
            PaymentStatus paymentStatus = payment.getStatus();

            if (paymentStatus == PaymentStatus.REFUND_PENDING) {
                throw new RuntimeException("PAYMENT_IN_PROGRESS");
            }

            if (paymentStatus == PaymentStatus.SUCCESS) {
                System.out.println("NOTE: Booking canceled. Refund amount calculated: " + refundAmount);
            }
        }

        booking.setStatus(BookingStatus.CANCELED);
        booking.setCancelReason(req.getCancelReason());
        booking.setUpdatedAt(now);

        bookingRepository.save(booking);

        return BookingCancelResponse.builder()
                .status(booking.getStatus())
                .refundAmount(refundAmount)
                .build();
    }

    @Transactional
    public BookingDetailResponse markAsPaid(String firebaseUid, UUID bookingId, BookingMarkPaidRequest req) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(me.getId())) {
            throw new SecurityException("FORBIDDEN");
        }

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("INVALID_STATUS: Can only mark as paid if status is PENDING_PAYMENT");
        }

        booking.setStatus(BookingStatus.AWAITING_CONFIRM);
        
        if (req != null) {
            booking.setRefundBankName(req.getRefundBankName());
            booking.setRefundAccountNumber(req.getRefundAccountNumber());
            booking.setRefundAccountName(req.getRefundAccountName());
        }
        
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        return getBookingDetail(firebaseUid, bookingId);
    }

    @Transactional
    public BookingDetailResponse confirmPayment(String firebaseUid, UUID bookingId) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        // Check if user is the owner of the venue
        UUID ownerId = booking.getCourt().getVenue().getOwner().getId();
        if (!ownerId.equals(me.getId())) {
            throw new SecurityException("FORBIDDEN: Only venue owner can confirm payment");
        }

        if (booking.getStatus() != BookingStatus.AWAITING_CONFIRM) {
            throw new RuntimeException("INVALID_STATUS: Can only confirm payment if status is AWAITING_CONFIRM");
        }

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        return getBookingDetail(firebaseUid, bookingId);
    }
}