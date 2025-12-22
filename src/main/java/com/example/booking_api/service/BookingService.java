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

import org.springframework.context.ApplicationEventPublisher;
import com.example.booking_api.event.BookingEvent;
import com.example.booking_api.entity.enums.NotificationType;
import org.springframework.scheduling.TaskScheduler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.Instant;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final CourtRepository courtRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository voucherRedemptionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TaskScheduler taskScheduler;

    // --- 1. LIST USER BOOKINGS ---
    public List<BookingListResponse> listUserBookings(String firebaseUid, BookingListRequest req) {
        try {
            User user = userRepository.findByFirebaseUid(firebaseUid)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            List<BookingStatus> statusList = null;
            if (req.getStatus() != null) {
                statusList = Collections.singletonList(req.getStatus());
            } else if (req.getStatuses() != null && !req.getStatuses().isEmpty()) {
                statusList = req.getStatuses();
            }

            List<Booking> bookings = bookingRepository.findByUserWithFilter(
                    user.getId(),
                    statusList,
                    req.getFrom(),
                    req.getTo()
            );

            return bookings.stream()
                    .map(b -> BookingListResponse.builder()
                            .id(b.getId())
                            .venue(b.getCourt().getVenue().getName())
                            .court(b.getCourt().getName())
                            .userName(b.getUser() != null ? b.getUser().getFullName() : "Unknown")
                            .startTime(b.getStartTime())
                            .endTime(b.getEndTime())
                            .totalPrice(b.getTotalAmount())
                            .status(b.getStatus() == null ? null : b.getStatus().name())
                            .refundAmount(b.getRefundAmount())
                            .refundBankName(b.getRefundBankName())
                            .refundAccountNumber(b.getRefundAccountNumber())
                            .refundAccountName(b.getRefundAccountName())
                            .build())
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("QUERY_ERROR", e);
        }
    }

    // --- 2. CREATE BOOKING ---
    @Transactional
    public BookingCreateResponse createBooking(String firebaseUid, BookingCreateRequest req) {
        User user = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        LocalDateTime start = req.getStartTime();
        LocalDateTime end = req.getEndTime();

        if (start == null || end == null) throw new IllegalArgumentException("start_time and end_time are required");
        if (!start.isBefore(end)) throw new IllegalArgumentException("start_time must be before end_time");

        Court court = courtRepository.findById(req.getCourtId()).orElseThrow(() -> new RuntimeException("Court not found"));
        if (Boolean.FALSE.equals(court.getIsActive())) throw new IllegalArgumentException("Court is not active");

        if (bookingRepository.countOverlappingBookings(court.getId(), start, end) > 0) {
            throw new RuntimeException("TIME_OVERLAP");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        Venue venue = court.getVenue();
        LocalDateTime slotStart = start;

        while (slotStart.isBefore(end)) {
            LocalDateTime slotEnd = slotStart.plusMinutes(30);
            if (slotEnd.isAfter(end)) slotEnd = end;

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
            totalAmount = totalAmount.add(slotBasePricePerHour.multiply(new BigDecimal("0.5")));
            slotStart = slotEnd;
        }

        totalAmount = totalAmount.setScale(2, RoundingMode.HALF_UP);
        BigDecimal depositAmount = "DEPOSIT".equalsIgnoreCase(req.getPaymentOption())
                ? totalAmount.multiply(new BigDecimal("0.3")).setScale(2, RoundingMode.HALF_UP)
                : totalAmount;
        BigDecimal amountToPay = depositAmount;

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCourt(court);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setTotalAmount(totalAmount);
        booking.setDepositAmount(depositAmount);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());

        Booking saved = bookingRepository.save(booking);

        // REMOVED: eventPublisher.publishEvent(new BookingEvent(this, saved, NotificationType.BOOKING_CREATED));
        // Moved to markAsPaid to prevent spamming owner
        scheduleBookingReminder(saved);

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

    // --- 3. GET AVAILABILITY ---
    public List<TimeSlotResponse> getAvailability(UUID courtId, LocalDate date) {
        Court court = courtRepository.findById(courtId).orElseThrow(() -> new RuntimeException("Court not found"));
        Venue venue = court.getVenue();
        List<Booking> bookings = bookingRepository.findByCourtAndDateRange(courtId, date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        List<TimeSlotResponse> slots = new ArrayList<>();
        LocalTime current = LocalTime.of(5, 0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        LocalDateTime now = LocalDateTime.now();

        while (current.isBefore(LocalTime.of(23, 0))) {
            LocalTime next = current.plusMinutes(30);
            LocalDateTime slotStart = LocalDateTime.of(date, current);
            LocalDateTime slotEnd = LocalDateTime.of(date, next);
            boolean isPast = slotStart.isBefore(now);

            boolean isBooked = bookings.stream().anyMatch(b -> {
                if (b.getStatus() == BookingStatus.CANCELED || b.getStatus() == BookingStatus.FAILED) return false;
                return slotStart.isBefore(b.getEndTime()) && slotEnd.isAfter(b.getStartTime());
            });

            BigDecimal slotPrice = court.getPricePerHour().divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
            // (Giản lược logic giá dynamic cho slot lẻ để code gọn)

            slots.add(TimeSlotResponse.builder()
                    .time(current.format(formatter))
                    .endTime(next.format(formatter))
                    .price(slotPrice)
                    .status((isBooked || isPast) ? "booked" : "available")
                    .build());
            current = next;
        }
        return slots;
    }

    // --- 4. GET BOOKING DETAIL ---
    public BookingDetailResponse getBookingDetail(String firebaseUid, UUID bookingId) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking b = bookingRepository.findDetailById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        boolean isUser = b.getUser() != null && b.getUser().getId().equals(me.getId());
        boolean isOwner = b.getCourt() != null && b.getCourt().getVenue().getOwner() != null && b.getCourt().getVenue().getOwner().getId().equals(me.getId());
        if (!isUser && !isOwner) throw new SecurityException("FORBIDDEN");

        BookingDetailResponse.PaymentItem paymentDto = null;
        if (b.getPayment() != null) {
            paymentDto = BookingDetailResponse.PaymentItem.builder()
                    .id(b.getPayment().getId())
                    .amount(b.getPayment().getAmount())
                    .status(b.getPayment().getStatus() != null ? b.getPayment().getStatus().name() : null)
                    .returnPayload(b.getPayment().getReturnPayload())
                    .build();
        }

        String voucherCode = voucherRedemptionRepository.findByBooking_Id(b.getId())
                .map(r -> r.getVoucher().getCode()).orElse(null);

        return BookingDetailResponse.builder()
                .id(b.getId())
                .venue(b.getCourt().getVenue().getName())
                .court(b.getCourt() == null ? null : b.getCourt().getName())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .createdAt(b.getCreatedAt())
                .totalPrice(b.getTotalAmount())
                .discountAmount(b.getDiscountAmount())
                .voucherCode(voucherCode)
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .payment(paymentDto)
                .bankBin(b.getCourt().getVenue().getBankBin())
                .bankAccountNumber(b.getCourt().getVenue().getBankAccountNumber())
                .bankAccountName(b.getCourt().getVenue().getBankAccountName())
                .refundAmount(b.getRefundAmount())
                .refundBankName(b.getRefundBankName())
                .refundAccountNumber(b.getRefundAccountNumber())
                .refundAccountName(b.getRefundAccountName())
                .build();
    }

    // --- 5. APPLY VOUCHER (Đã khôi phục logic) ---
    @Transactional
    public BookingApplyVoucherResponse applyVoucher(String firebaseUid, UUID bookingId, BookingApplyVoucherRequest req) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(me.getId())) throw new SecurityException("FORBIDDEN");

        if (booking.getStatus() != BookingStatus.PENDING && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("BOOKING_STATUS_NOT_ALLOWED");
        }

        if (voucherRedemptionRepository.existsByBooking_Id(booking.getId())) {
            throw new RuntimeException("VOUCHER_ALREADY_APPLIED");
        }

        BigDecimal originalPrice = booking.getTotalAmount();
        String code = req.getVoucherCode().trim();
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new RuntimeException("VOUCHER_NOT_FOUND"));

        // Validate voucher
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime voucherValidFrom = voucher.getValidFrom() != null ? voucher.getValidFrom().toLocalDateTime() : null;
        LocalDateTime voucherValidTo = voucher.getValidTo() != null ? voucher.getValidTo().toLocalDateTime() : null;

        if (Boolean.FALSE.equals(voucher.getActive())) throw new RuntimeException("VOUCHER_EXPIRED");
        if (voucherValidFrom != null && now.isBefore(voucherValidFrom)) throw new RuntimeException("VOUCHER_EXPIRED");
        if (voucherValidTo != null && now.isAfter(voucherValidTo)) throw new RuntimeException("VOUCHER_EXPIRED");
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) throw new RuntimeException("VOUCHER_EXPIRED");
        if (voucher.getMinOrderAmount() != null && originalPrice.compareTo(voucher.getMinOrderAmount()) < 0) throw new RuntimeException("MIN_ORDER_NOT_REACHED");

        if (voucher.getOwner() != null) {
            UUID venueOwnerId = booking.getCourt().getVenue().getOwner().getId();
            if (!venueOwnerId.equals(voucher.getOwner().getId())) throw new RuntimeException("VOUCHER_NOT_APPLICABLE");
        }

        BigDecimal discount = BigDecimal.ZERO;
        if (voucher.getType() == VoucherType.FIXED) {
            discount = voucher.getValue();
        } else if (voucher.getType() == VoucherType.PERCENT) {
            discount = originalPrice.multiply(voucher.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        if (discount.compareTo(BigDecimal.ZERO) < 0) discount = BigDecimal.ZERO;
        if (discount.compareTo(originalPrice) > 0) discount = originalPrice;

        BigDecimal newTotal = originalPrice.subtract(discount).setScale(2, RoundingMode.HALF_UP);

        booking.setDiscountAmount(discount);
        booking.setTotalAmount(newTotal);
        booking.setUpdatedAt(now);

        voucher.setUsedCount(voucher.getUsedCount() + 1);
        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setVoucher(voucher);
        redemption.setUser(me);
        redemption.setBooking(booking);
        redemption.setDiscountValue(discount);
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

    // --- 6. REMOVE VOUCHER ---
    @Transactional
    public BookingApplyVoucherResponse removeVoucher(String firebaseUid, UUID bookingId) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));
        if (!booking.getUser().getId().equals(me.getId())) throw new SecurityException("FORBIDDEN");

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

    // --- 7. CANCEL BOOKING (Đã khôi phục logic hoàn tiền) ---
    @Transactional
    public BookingCancelResponse cancelBooking(String firebaseUid, UUID bookingId, BookingCancelRequest req) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        boolean isUser = booking.getUser().getId().equals(me.getId());
        boolean isOwner = booking.getCourt().getVenue().getOwner().getId().equals(me.getId());
        if (!isUser && !isOwner) throw new SecurityException("FORBIDDEN");

        if (booking.getStatus() == BookingStatus.CANCELED) throw new RuntimeException("ALREADY_CANCELED");
        if (booking.getStatus() == BookingStatus.COMPLETED) throw new RuntimeException("CANCEL_WINDOW_PASSED");

        LocalDateTime now = LocalDateTime.now();
        if (booking.getStartTime() != null && !now.isBefore(booking.getStartTime())) {
            throw new RuntimeException("Cannot cancel past booking");
        }

        // Logic tính toán hoàn tiền
        long minutesUntilStart = Duration.between(now, booking.getStartTime()).toMinutes();
        BigDecimal refundPercentage;
        if (minutesUntilStart > 360) {
            refundPercentage = BigDecimal.ONE;
        } else if (minutesUntilStart >= 120) {
            refundPercentage = new BigDecimal("0.5");
        } else {
            refundPercentage = BigDecimal.ZERO;
        }

        BigDecimal paidAmount = booking.getDepositAmount();
        if (paidAmount == null) paidAmount = booking.getTotalAmount().multiply(new BigDecimal("0.3"));
        BigDecimal refundAmount = paidAmount.multiply(refundPercentage).setScale(2, RoundingMode.HALF_UP);

        Payment payment = booking.getPayment();
        if (payment != null && payment.getStatus() == PaymentStatus.REFUND_PENDING) {
            throw new RuntimeException("PAYMENT_IN_PROGRESS");
        }

        booking.setRefundAmount(refundAmount);
        booking.setRefundBankName(req.getBankName());
        booking.setRefundAccountNumber(req.getAccountNumber());
        booking.setRefundAccountName(req.getAccountHolderName());

        if (refundAmount.compareTo(BigDecimal.ZERO) > 0) {
            booking.setStatus(BookingStatus.REFUND_PENDING);
            // 👇 Notify Owner: REFUND_REQUESTED
            eventPublisher.publishEvent(new BookingEvent(this, booking, NotificationType.REFUND_REQUESTED));
        } else {
            booking.setStatus(BookingStatus.CANCELED);
            // Notify User: BOOKING_CANCELLED (Already handled by BookingListener if needed, or explicitly here)
            // Note: BookingListener handles BOOKING_CANCELLED.
            eventPublisher.publishEvent(new BookingEvent(this, booking, NotificationType.BOOKING_CANCELLED));
        }

        booking.setCancelReason(req.getCancelReason());
        booking.setUpdatedAt(now);
        bookingRepository.save(booking);

        return BookingCancelResponse.builder()
                .status(booking.getStatus())
                .refundAmount(refundAmount)
                .build();
    }

    // --- 8. MARK AS PAID ---
    @Transactional
    public BookingDetailResponse markAsPaid(String firebaseUid, UUID bookingId, BookingMarkPaidRequest req) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));
        if (!booking.getUser().getId().equals(me.getId())) throw new SecurityException("FORBIDDEN");

        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) throw new RuntimeException("INVALID_STATUS");

        booking.setStatus(BookingStatus.AWAITING_CONFIRM);
        if (req != null) {
            booking.setRefundBankName(req.getRefundBankName());
            booking.setRefundAccountNumber(req.getRefundAccountNumber());
            booking.setRefundAccountName(req.getRefundAccountName());
        }
        booking.setUpdatedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        // 👇 Notify Owner when User claims they have paid
        eventPublisher.publishEvent(new BookingEvent(this, saved, NotificationType.BOOKING_CREATED));

        return getBookingDetail(firebaseUid, bookingId);
    }

    // --- 9. CONFIRM PAYMENT ---
    @Transactional
    public BookingDetailResponse confirmPayment(String firebaseUid, UUID bookingId) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));

        UUID ownerId = booking.getCourt().getVenue().getOwner().getId();
        if (!ownerId.equals(me.getId())) throw new SecurityException("FORBIDDEN: Only venue owner can confirm payment");

        if (booking.getStatus() != BookingStatus.AWAITING_CONFIRM) throw new RuntimeException("INVALID_STATUS");

        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingEvent(this, booking, NotificationType.BOOKING_CONFIRMED));

        return getBookingDetail(firebaseUid, bookingId);
    }

    // --- 10. LIST OWNER BOOKINGS ---
    public List<BookingListResponse> listOwnerBookings(String firebaseUid, BookingListRequest req) {
        User owner = userRepository.findByFirebaseUid(firebaseUid)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<BookingStatus> filterStatuses = req.getStatuses();
        if ((filterStatuses == null || filterStatuses.isEmpty()) && req.getStatus() != null) {
            filterStatuses = Collections.singletonList(req.getStatus());
        }

        List<Booking> bookings = bookingRepository.findByOwnerWithFilter(
                owner.getId(),
                req.getVenueId(),
                filterStatuses,
                req.getFrom(),
                req.getTo()
        );

        return bookings.stream().map(b -> BookingListResponse.builder()
                .id(b.getId())
                .venue(b.getCourt().getVenue().getName())
                .court(b.getCourt().getName())
                .userName(b.getUser() != null ? b.getUser().getFullName() : "Unknown")
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .totalPrice(b.getTotalAmount())
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .refundAmount(b.getRefundAmount())
                .refundBankName(b.getRefundBankName())
                .refundAccountNumber(b.getRefundAccountNumber())
                .refundAccountName(b.getRefundAccountName())
                .build()).toList();
    }

    // --- 11. LIST OWNER PENDING ---
    public List<BookingListResponse> listOwnerPendingBookings(String firebaseUid) {
        User owner = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        List<Booking> bookings = bookingRepository.findByOwnerAndStatus(owner.getId(), BookingStatus.AWAITING_CONFIRM);

        return bookings.stream().map(b -> BookingListResponse.builder()
                .id(b.getId())
                .venue(b.getCourt().getVenue().getName())
                .court(b.getCourt().getName())
                .userName(b.getUser() != null ? b.getUser().getFullName() : "Unknown")
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
                .totalPrice(b.getTotalAmount())
                .status(b.getStatus() != null ? b.getStatus().name() : null)
                .refundAmount(b.getRefundAmount())
                .refundBankName(b.getRefundBankName())
                .refundAccountNumber(b.getRefundAccountNumber())
                .refundAccountName(b.getRefundAccountName())
                .build()).toList();
    }

    // --- 12. REVENUE STATS ---
    public List<Map<String, Object>> getRevenueStats(String firebaseUid, LocalDateTime from, LocalDateTime to) {
        User owner = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        List<BookingStatus> statuses = List.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED);

        List<Booking> bookings = bookingRepository.findByOwnerWithFilter(owner.getId(), null, statuses, from, to);

        Map<LocalDate, BigDecimal> dailyRevenue = new TreeMap<>();
        Map<LocalDate, Integer> dailyCount = new TreeMap<>();

        for (Booking b : bookings) {
            LocalDate date = b.getStartTime().toLocalDate();
            dailyRevenue.put(date, dailyRevenue.getOrDefault(date, BigDecimal.ZERO).add(b.getTotalAmount()));
            dailyCount.put(date, dailyCount.getOrDefault(date, 0) + 1);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        if (from != null && to != null) {
            for (LocalDate date = from.toLocalDate(); !date.isAfter(to.toLocalDate()); date = date.plusDays(1)) {
                result.add(Map.of(
                        "date", date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                        "value", dailyRevenue.getOrDefault(date, BigDecimal.ZERO),
                        "count", dailyCount.getOrDefault(date, 0)
                ));
            }
        } else {
            dailyRevenue.forEach((d, v) -> result.add(Map.of("date", d.toString(), "value", v, "count", dailyCount.get(d))));
        }
        return result;
    }

    // --- 13. CONFIRM REFUND ---
    @Transactional
    public BookingDetailResponse confirmRefund(String firebaseUid, UUID bookingId) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));
        UUID ownerId = booking.getCourt().getVenue().getOwner().getId();
        if (!ownerId.equals(me.getId())) throw new SecurityException("FORBIDDEN");

        if (booking.getStatus() != BookingStatus.REFUND_PENDING) throw new RuntimeException("INVALID_STATUS");

        booking.setStatus(BookingStatus.CANCELED);
        booking.setUpdatedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        // 👇 Notify User: REFUND_COMPLETED
        eventPublisher.publishEvent(new BookingEvent(this, saved, NotificationType.REFUND_COMPLETED));

        return getBookingDetail(firebaseUid, bookingId);
    }

    // --- 14. DECLINE BOOKING ---
    @Transactional
    public BookingDetailResponse declineBooking(String firebaseUid, UUID bookingId) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new RuntimeException("Booking not found"));
        UUID ownerId = booking.getCourt().getVenue().getOwner().getId();
        if (!ownerId.equals(me.getId())) throw new SecurityException("FORBIDDEN: Only venue owner can decline");

        if (booking.getStatus() != BookingStatus.AWAITING_CONFIRM && booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new RuntimeException("INVALID_STATUS");
        }

        booking.setStatus(BookingStatus.CANCELED);
        booking.setCancelReason("Owner declined");
        booking.setUpdatedAt(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        eventPublisher.publishEvent(new BookingEvent(this, saved, NotificationType.BOOKING_CANCELLED));

        return getBookingDetail(firebaseUid, bookingId);
    }

    private void scheduleBookingReminder(Booking booking) {
        if (booking.getUser() == null || booking.getStartTime() == null) return;
        long secondsBefore = 900;
        Instant remindTime = booking.getStartTime().atZone(ZoneId.systemDefault()).toInstant().minusSeconds(secondsBefore);
        if (remindTime.isBefore(Instant.now())) return;

        taskScheduler.schedule(() -> {
            try {
                eventPublisher.publishEvent(new BookingEvent(this, booking, NotificationType.REMINDER));
            } catch (Exception e) { e.printStackTrace(); }
        }, remindTime);
    }
}