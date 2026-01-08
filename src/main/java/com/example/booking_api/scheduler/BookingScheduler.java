package com.example.booking_api.scheduler;

import com.example.booking_api.entity.Booking;
import com.example.booking_api.entity.enums.BookingStatus;
import com.example.booking_api.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingScheduler {

    private final BookingRepository bookingRepository;

    @Scheduled(fixedRate = 60000) // Run every 60 seconds
    @Transactional
    public void autoCompleteBookings() {
        log.info("Running auto-complete bookings task...");
        LocalDateTime now = LocalDateTime.now();

        List<Booking> expiredBookings = bookingRepository.findByStatusAndEndTimeBefore(
                BookingStatus.CONFIRMED,
                now
        );

        if (!expiredBookings.isEmpty()) {
            for (Booking booking : expiredBookings) {
                booking.setStatus(BookingStatus.COMPLETED);
                log.info("Auto-completed booking ID: {}", booking.getId());
            }
            bookingRepository.saveAll(expiredBookings);
            log.info("Completed {} bookings.", expiredBookings.size());
        } else {
            log.info("No bookings to complete.");
        }
    }
}
