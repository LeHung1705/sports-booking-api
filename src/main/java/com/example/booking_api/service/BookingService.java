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
import java.time.*;
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
                            .court(b.getCourt().getName())
                            .startTime(b.getStartTime())
                            .status(b.getStatus() == null ? null : b.getStatus().name())
                            .build())
                    .toList();

        } catch (Exception e) {

            throw new RuntimeException("QUERY_ERROR", e);
        }
    }

    @Transactional
    public BookingCreateResponse createBooking(String firebaseUid, BookingCreateRequest req) {
        User user = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));

        OffsetDateTime start = req.getStartTime();
        OffsetDateTime end = req.getEndTime();

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
            // để controller map thành 409
            throw new RuntimeException("TIME_OVERLAP");
        }

        Duration duration = Duration.between(start, end);
        long minutes = duration.toMinutes();
        if (minutes < 0) {
            throw new IllegalArgumentException("duration must be greater than zero");
        }

        // Determine price based on dynamic pricing if configured
        BigDecimal pricePerHour = court.getPricePerHour();
        if (pricePerHour == null) {
             // Try to get base price from venue if court price missing? Or fail. 
             // For now, throw exception as before.
             throw new IllegalArgumentException("Court price is not configured");
        }
        
        // NOTE: For a booking spanning multiple hours, we should ideally calculate weighted price 
        // based on slots. For simplicity here, we stick to base price or maybe user expects us to 
        // just apply the dynamic logic? 
        // Given "apply calculation for each slot" in availability, we should probably do it here too 
        // to be consistent. But for this specific task, the prompt only emphasized @Transactional 
        // and availability. We'll stick to the existing logic for createBooking to avoid breaking changes
        // unless explicitly asked, but keeping in mind the prompt's "Auditing" note.
        
        BigDecimal hours = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = pricePerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);

        // Create booking entity

        OffsetDateTime now =  OffsetDateTime.now();

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCourt(court);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setTotalAmount(totalAmount);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setCreatedAt(now);
        booking.setUpdatedAt(now);

        Booking saved = bookingRepository.save(booking);
        return BookingCreateResponse.builder()
                .id(saved.getId())
                .totalAmount(saved.getTotalAmount())
                .status(saved.getStatus())
                .build();
    }
    
    public List<TimeSlotResponse> getAvailability(UUID courtId, LocalDate date) {
        Court court = courtRepository.findById(courtId)
                .orElseThrow(() -> new RuntimeException("Court not found"));
        Venue venue = court.getVenue();

        OffsetDateTime startOfDay = date.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<Booking> bookings = bookingRepository.findByCourtAndDateRange(courtId, startOfDay, endOfDay);

        List<TimeSlotResponse> slots = new ArrayList<>();
        LocalTime current = LocalTime.of(5, 0); // 05:00
        LocalTime closeTime = LocalTime.of(22, 0); // 22:00

        while (current.isBefore(closeTime)) {
            LocalTime next = current.plusMinutes(30); // 30 mins slots

            OffsetDateTime slotStart = date.atTime(current).atOffset(ZoneOffset.UTC);
            OffsetDateTime slotEnd = date.atTime(next).atOffset(ZoneOffset.UTC);

            // 1. Determine Status
            boolean isBooked = bookings.stream().anyMatch(b ->
                    !(b.getEndTime().isBefore(slotStart) || b.getEndTime().isEqual(slotStart) ||
                      b.getStartTime().isAfter(slotEnd) || b.getStartTime().isEqual(slotEnd))
            );
            
            // 2. Determine Price
            BigDecimal price = court.getPricePerHour(); // Default
            
            // Apply Venue Pricing Config if matches
            if (venue.getPricingConfig() != null) {
                for (PricingRule rule : venue.getPricingConfig()) {
                    // If slot is fully within rule range (or overlaps? Usually start time check is enough for slot)
                    // Rule: 17:00 - 22:00. Slot: 17:00 - 17:30.
                    if (!current.isBefore(rule.getStartTime()) && !next.isAfter(rule.getEndTime())) {
                        price = rule.getPricePerHour();
                        break; // Found specific price
                    }
                }
            }
            
            BigDecimal slotPrice = price.multiply(BigDecimal.valueOf(0.5)).setScale(2, RoundingMode.HALF_UP);

            slots.add(TimeSlotResponse.builder()
                    .time(current)
                    .endTime(next)
                    .price(slotPrice) // Price for this 30min slot
                    .status(isBooked ? "BOOKED" : "AVAILABLE")
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
                .court(b.getCourt() == null ? null : b.getCourt().getName())
                .startTime(b.getStartTime())
                .endTime(b.getEndTime())
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

        String code = req.getVoucherCode().trim();
        Voucher voucher = voucherRepository.findByCodeIgnoreCase(code).orElseThrow(() -> new RuntimeException("VOUCHER_NOT_FOUND"));

        // Validate voucher
        OffsetDateTime now = OffsetDateTime.now();

        if (Boolean.FALSE.equals(voucher.getActive())) {
            throw new RuntimeException("VOUCHER_EXPIRED");
        }

        if (voucher.getValidFrom() != null && now.isBefore(voucher.getValidFrom())) {
            throw new RuntimeException("VOUCHER_EXPIRED");
        }

        if (voucher.getValidTo() != null && now.isAfter(voucher.getValidTo())) {
            throw new RuntimeException("VOUCHER_EXPIRED");
        }

        if (voucher.getUsageLimit() != null
                && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new RuntimeException("VOUCHER_EXPIRED");
        }

        if (voucher.getMinOrderAmount() != null
                && originalPrice.compareTo(voucher.getMinOrderAmount()) < 0) {
            throw new RuntimeException("MIN_ORDER_NOT_REACHED");
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
        redemption.setCreatedAt(now);

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
        booking.setUpdatedAt(OffsetDateTime.now());


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

        OffsetDateTime now = OffsetDateTime.now();
        if (booking.getStartTime() != null && !now.isBefore(booking.getStartTime())) {
            throw new RuntimeException("CANCEL_WINDOW_PASSED");
        }

        Payment payment = booking.getPayment();
        if (payment != null) {
            PaymentStatus paymentStatus = payment.getStatus();

            if (paymentStatus == PaymentStatus.REFUND_PENDING) {
                throw new RuntimeException("PAYMENT_IN_PROGRESS");
            }

            // Refund here
            if (paymentStatus == PaymentStatus.SUCCESS) {
                System.out.println("NOTE: Booking canceled with SUCCESS payment - refund not implemented yet");
            }
        }

        booking.setStatus(BookingStatus.CANCELED);
        booking.setCancelReason(req.getCancelReason());
        booking.setUpdatedAt(now);

        bookingRepository.save(booking);

        return new BookingCancelResponse(booking.getStatus());
    }
}