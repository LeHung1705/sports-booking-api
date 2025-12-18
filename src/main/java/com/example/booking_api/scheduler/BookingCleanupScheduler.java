package com.example.booking_api.scheduler;

import com.example.booking_api.entity.Booking;
import com.example.booking_api.entity.enums.BookingStatus;
import com.example.booking_api.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BookingCleanupScheduler {

    private final BookingRepository bookingRepository;

    @Scheduled(fixedRate = 60000) // Run every 1 minute
    @Transactional
    public void cancelExpiredPendingBookings() {
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        List<Booking> expiredBookings = bookingRepository.findByStatusAndCreatedAtBefore(BookingStatus.PENDING_PAYMENT, tenMinutesAgo);

        if (!expiredBookings.isEmpty()) {
            System.out.println("Scheduler: Found " + expiredBookings.size() + " expired pending bookings. Cancelling...");
            for (Booking booking : expiredBookings) {
                booking.setStatus(BookingStatus.CANCELED); // Or FAILED? CANCELED seems safer for now.
                booking.setCancelReason("System: Payment timeout (10 mins)");
                booking.setUpdatedAt(LocalDateTime.now());
            }
            bookingRepository.saveAll(expiredBookings);
        }
    }
}
