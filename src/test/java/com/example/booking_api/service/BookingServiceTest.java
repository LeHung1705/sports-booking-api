package com.example.booking_api.service;

import com.example.booking_api.dto.booking.*;
import com.example.booking_api.entity.*;
import com.example.booking_api.entity.enums.BookingStatus;
import com.example.booking_api.entity.enums.PaymentStatus;
import com.example.booking_api.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourtRepository courtRepository;

    @Mock
    private VoucherRepository voucherRepository;

    @Mock
    private VoucherRedemptionRepository voucherRedemptionRepository;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Court testCourt;
    private Venue testVenue;
    private Booking testBooking;
    private UUID bookingId;

    @BeforeEach
    void setUp() {
        bookingId = UUID.randomUUID();

        testUser = new User();
        testUser.setId(UUID.randomUUID());
        testUser.setFirebaseUid("test_firebase_uid_123");
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");

        testVenue = new Venue();
        testVenue.setId(UUID.randomUUID());
        testVenue.setName("Test Venue");
        testVenue.setBankBin("970423");
        testVenue.setBankAccountNumber("1234567890");
        testVenue.setBankAccountName("Test Account");
        testVenue.setOwner(testUser);

        testCourt = new Court();
        testCourt.setId(UUID.randomUUID());
        testCourt.setVenue(testVenue);
        testCourt.setName("Court 1");
        testCourt.setPricePerHour(new BigDecimal("200000.00"));
        testCourt.setIsActive(true);

        testBooking = new Booking();
        testBooking.setId(bookingId);
        testBooking.setUser(testUser);
        testBooking.setCourt(testCourt);
        testBooking.setStartTime(LocalDateTime.now().plusHours(3));
        testBooking.setEndTime(LocalDateTime.now().plusHours(4));
        testBooking.setTotalAmount(new BigDecimal("200000.00"));
        testBooking.setDepositAmount(new BigDecimal("60000.00")); // 30%
        testBooking.setStatus(BookingStatus.PENDING_PAYMENT);
        testBooking.setCreatedAt(LocalDateTime.now());
        testBooking.setUpdatedAt(LocalDateTime.now());
    }

    // Test 1: Create Booking Success
    @Test
    void createBooking_Success() {
        // Arrange
        BookingCreateRequest request = new BookingCreateRequest();
        request.setCourtId(testCourt.getId());
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setEndTime(LocalDateTime.now().plusHours(3));
        request.setPaymentOption("DEPOSIT");

        when(userRepository.findByFirebaseUid("test_firebase_uid_123"))
                .thenReturn(Optional.of(testUser));
        when(courtRepository.findById(testCourt.getId()))
                .thenReturn(Optional.of(testCourt));
        when(bookingRepository.countOverlappingBookings(any(), any(), any()))
                .thenReturn(0L);
        when(bookingRepository.save(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking booking = invocation.getArgument(0);
                    booking.setId(UUID.randomUUID());
                    booking.setStatus(BookingStatus.PENDING_PAYMENT);
                    return booking;
                });

        // Act
        BookingCreateResponse response = bookingService.createBooking("test_firebase_uid_123", request);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(BookingStatus.PENDING_PAYMENT, response.getStatus());
        assertTrue(response.getTotalAmount().compareTo(BigDecimal.ZERO) > 0);
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }

    // Test 2: Create Booking - Court Not Found
    @Test
    void createBooking_CourtNotFound() {
        // Arrange
        BookingCreateRequest request = new BookingCreateRequest();
        request.setCourtId(UUID.randomUUID());
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setEndTime(LocalDateTime.now().plusHours(3));

        when(userRepository.findByFirebaseUid("test_firebase_uid_123"))
                .thenReturn(Optional.of(testUser));
        when(courtRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking("test_firebase_uid_123", request);
        });

        assertTrue(exception.getMessage().contains("Court not found"));
    }

    // Test 3: Create Booking - Time Overlap
    @Test
    void createBooking_TimeOverlap() {
        // Arrange
        BookingCreateRequest request = new BookingCreateRequest();
        request.setCourtId(testCourt.getId());
        request.setStartTime(LocalDateTime.now().plusHours(2));
        request.setEndTime(LocalDateTime.now().plusHours(3));

        when(userRepository.findByFirebaseUid("test_firebase_uid_123"))
                .thenReturn(Optional.of(testUser));
        when(courtRepository.findById(testCourt.getId()))
                .thenReturn(Optional.of(testCourt));
        when(bookingRepository.countOverlappingBookings(any(), any(), any()))
                .thenReturn(1L);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking("test_firebase_uid_123", request);
        });

        assertEquals("TIME_OVERLAP", exception.getMessage());
    }

    // Test 4: Get Availability Success
    @Test
    void getAvailability_Success() {
        // Arrange
        UUID courtId = testCourt.getId();
        LocalDate date = LocalDate.now().plusDays(1);

        when(courtRepository.findById(courtId))
                .thenReturn(Optional.of(testCourt));
        when(bookingRepository.findByCourtAndDateRange(eq(courtId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Collections.emptyList());

        // Act
        List<TimeSlotResponse> slots = bookingService.getAvailability(courtId, date);

        // Assert
        assertNotNull(slots);
        assertFalse(slots.isEmpty());
        // Should have slots from 5:00 to 23:00
        assertTrue(slots.size() >= 20);
    }

    // Test 5: Cancel Booking Success (More than 6 hours) - FIXED
    @Test
    void cancelBooking_Success_FullRefund() {
        // Arrange
        BookingCancelRequest cancelRequest = new BookingCancelRequest();
        cancelRequest.setCancelReason("Change of plans");
        cancelRequest.setBankName("VCB");
        cancelRequest.setAccountNumber("1234567890");
        cancelRequest.setAccountHolderName("Test User");

        // Set booking start time more than 6 hours from now
        testBooking.setStartTime(LocalDateTime.now().plusHours(7));
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setDepositAmount(new BigDecimal("60000.00"));

        when(userRepository.findByFirebaseUid("test_firebase_uid_123"))
                .thenReturn(Optional.of(testUser));
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(testBooking));

        // Act
        BookingCancelResponse response = bookingService.cancelBooking("test_firebase_uid_123", bookingId, cancelRequest);

        // Assert
        assertNotNull(response);
        assertEquals(BookingStatus.REFUND_PENDING, response.getStatus());
        // So sánh với scale 2
        assertEquals(new BigDecimal("60000.00"), response.getRefundAmount());
    }

    // Test 6: Cancel Booking - 50% Refund (2-6 hours)
    @Test
    void cancelBooking_PartialRefund() {
        // Arrange
        BookingCancelRequest cancelRequest = new BookingCancelRequest();
        cancelRequest.setCancelReason("Emergency");
        cancelRequest.setBankName("VCB");
        cancelRequest.setAccountNumber("1234567890");
        cancelRequest.setAccountHolderName("Test User");

        // Set booking start time 3 hours from now (50% refund)
        testBooking.setStartTime(LocalDateTime.now().plusHours(3));
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setDepositAmount(new BigDecimal("60000.00"));

        when(userRepository.findByFirebaseUid("test_firebase_uid_123"))
                .thenReturn(Optional.of(testUser));
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(testBooking));

        // Act
        BookingCancelResponse response = bookingService.cancelBooking("test_firebase_uid_123", bookingId, cancelRequest);

        // Assert - FIXED
        assertNotNull(response);
        assertEquals(BookingStatus.REFUND_PENDING, response.getStatus());
        // 50% of 60000 = 30000
        assertEquals(new BigDecimal("30000.00"), response.getRefundAmount());
    }

    // Test 7: Cancel Booking - No Refund (Less than 2 hours)
    @Test
    void cancelBooking_NoRefund() {
        // Arrange
        BookingCancelRequest cancelRequest = new BookingCancelRequest();
        cancelRequest.setCancelReason("Too late");
        cancelRequest.setBankName("VCB");
        cancelRequest.setAccountNumber("1234567890");
        cancelRequest.setAccountHolderName("Test User");

        // Set booking start time 1 hour from now (0% refund)
        testBooking.setStartTime(LocalDateTime.now().plusHours(1));
        testBooking.setStatus(BookingStatus.CONFIRMED);
        testBooking.setDepositAmount(new BigDecimal("60000.00"));

        when(userRepository.findByFirebaseUid("test_firebase_uid_123"))
                .thenReturn(Optional.of(testUser));
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(testBooking));

        // Act
        BookingCancelResponse response = bookingService.cancelBooking("test_firebase_uid_123", bookingId, cancelRequest);

        // Assert
        assertNotNull(response);
        assertEquals(BookingStatus.CANCELED, response.getStatus());
        assertEquals(BigDecimal.ZERO.setScale(2), response.getRefundAmount());
    }

    // Test 8: Cancel Booking - Already Canceled
    @Test
    void cancelBooking_AlreadyCanceled() {
        // Arrange
        BookingCancelRequest cancelRequest = new BookingCancelRequest();
        cancelRequest.setCancelReason("Test");

        testBooking.setStatus(BookingStatus.CANCELED);

        when(userRepository.findByFirebaseUid("test_firebase_uid_123"))
                .thenReturn(Optional.of(testUser));
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(testBooking));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.cancelBooking("test_firebase_uid_123", bookingId, cancelRequest);
        });

        assertEquals("ALREADY_CANCELED", exception.getMessage());
    }

    // Test 9: Get Booking Detail Success
    @Test
    void getBookingDetail_Success() {
        // Arrange
        Payment payment = new Payment();
        payment.setId(UUID.randomUUID());
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setAmount(new BigDecimal("200000.00"));

        testBooking.setPayment(payment);

        when(userRepository.findByFirebaseUid("test_firebase_uid_123"))
                .thenReturn(Optional.of(testUser));
        when(bookingRepository.findDetailById(bookingId))
                .thenReturn(Optional.of(testBooking));

        // Act
        BookingDetailResponse response = bookingService.getBookingDetail("test_firebase_uid_123", bookingId);

        // Assert
        assertNotNull(response);
        assertEquals(bookingId, response.getId());
        assertEquals(testVenue.getName(), response.getVenue());
        assertEquals(testCourt.getName(), response.getCourt());
    }

    // Test 10: Get Booking Detail - Forbidden
    @Test
    void getBookingDetail_Forbidden() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        otherUser.setFirebaseUid("other_user");

        when(userRepository.findByFirebaseUid("other_user"))
                .thenReturn(Optional.of(otherUser));
        when(bookingRepository.findDetailById(bookingId))
                .thenReturn(Optional.of(testBooking));

        // Act & Assert
        SecurityException exception = assertThrows(SecurityException.class, () -> {
            bookingService.getBookingDetail("other_user", bookingId);
        });

        assertEquals("FORBIDDEN", exception.getMessage());
    }

    // Test 11: Apply Voucher Success
    @Test
    void applyVoucher_Success() {
        // Arrange
        Voucher voucher = new Voucher();
        voucher.setId(UUID.randomUUID());
        voucher.setCode("TEST10");
        voucher.setType(com.example.booking_api.entity.enums.VoucherType.FIXED);
        voucher.setValue(new BigDecimal("10000.00"));
        voucher.setMinOrderAmount(new BigDecimal("50000.00"));
        voucher.setActive(true);
        voucher.setUsedCount(0);
        voucher.setOwner(testUser);

        BookingApplyVoucherRequest request = new BookingApplyVoucherRequest();
        request.setVoucherCode("TEST10");

        testBooking.setStatus(BookingStatus.PENDING);

        when(userRepository.findByFirebaseUid("test_firebase_uid_123"))
                .thenReturn(Optional.of(testUser));
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(testBooking));
        when(voucherRedemptionRepository.existsByBooking_Id(bookingId))
                .thenReturn(false);
        when(voucherRepository.findByCodeIgnoreCase("TEST10"))
                .thenReturn(Optional.of(voucher));
        when(voucherRepository.save(any(Voucher.class)))
                .thenReturn(voucher);
        when(bookingRepository.save(any(Booking.class)))
                .thenReturn(testBooking);
        when(voucherRedemptionRepository.save(any(VoucherRedemption.class)))
                .thenReturn(new VoucherRedemption());

        // Act
        BookingApplyVoucherResponse response = bookingService.applyVoucher("test_firebase_uid_123", bookingId, request);

        // Assert
        assertNotNull(response);
        assertEquals("TEST10", response.getVoucherCode());
        assertEquals(new BigDecimal("10000.00"), response.getDiscountValue());
    }

    // Test 12: Apply Voucher - Already Applied
    @Test
    void applyVoucher_AlreadyApplied() {
        // Arrange
        BookingApplyVoucherRequest request = new BookingApplyVoucherRequest();
        request.setVoucherCode("TEST10");

        when(userRepository.findByFirebaseUid("test_firebase_uid_123"))
                .thenReturn(Optional.of(testUser));
        when(bookingRepository.findById(bookingId))
                .thenReturn(Optional.of(testBooking));
        when(voucherRedemptionRepository.existsByBooking_Id(bookingId))
                .thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.applyVoucher("test_firebase_uid_123", bookingId, request);
        });

        assertEquals("VOUCHER_ALREADY_APPLIED", exception.getMessage());
    }
}