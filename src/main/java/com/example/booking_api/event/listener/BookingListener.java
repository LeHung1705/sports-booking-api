package com.example.booking_api.event.listener;

import com.example.booking_api.entity.Booking;
import com.example.booking_api.entity.User;
import com.example.booking_api.entity.enums.NotificationType;
import com.example.booking_api.event.BookingEvent;
import com.example.booking_api.repository.BookingRepository;
import com.example.booking_api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation; // 👈 Import cái này
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class BookingListener {

    private final NotificationService notificationService;
    private final BookingRepository bookingRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // 👇 SỬA DÒNG NÀY: Thêm propagation = Propagation.REQUIRES_NEW
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleBookingEvent(BookingEvent event) {
        try {
            // Tìm lại Booking từ Database (Để tránh lỗi Lazy Loading)
            Booking booking = bookingRepository.findById(event.getBooking().getId())
                    .orElse(null);

            if (booking == null) {
                System.err.println("❌ Không tìm thấy booking trong Listener ID: " + event.getBooking().getId());
                return;
            }

            var type = event.getType();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM");

            // 1. CÓ ĐƠN MỚI -> Báo Owner
            if (type == NotificationType.BOOKING_CREATED) {
                User owner = booking.getCourt().getVenue().getOwner();
                if (owner != null) {
                    String title = "🔔 Có đơn đặt sân mới!";
                    String body = "Khách hàng " + booking.getUser().getFullName()
                            + " vừa đặt sân " + booking.getCourt().getName()
                            + " lúc " + booking.getStartTime().format(formatter);

                    notificationService.sendAndSaveNotification(owner, title, body, booking.getId(), type);
                }
            }

            // 2. ĐÃ XÁC NHẬN -> Báo User
            else if (type == NotificationType.BOOKING_CONFIRMED) {
                User user = booking.getUser();
                if (user != null) {
                    String title = "✅ Đặt sân thành công!";
                    String body = "Chủ sân đã xác nhận lịch đá tại " + booking.getCourt().getName()
                            + " (" + booking.getStartTime().format(formatter) + ")";

                    notificationService.sendAndSaveNotification(user, title, body, booking.getId(), type);
                }
            }

            // 3. ĐÃ HỦY / TỪ CHỐI -> Báo User
            else if (type == NotificationType.BOOKING_CANCELLED) {
                User user = booking.getUser();
                if (user != null) {
                    String title = "❌ Đơn đặt sân đã bị hủy";
                    String body = "Lịch đặt tại " + booking.getCourt().getName()
                            + " đã bị hủy hoặc từ chối.";

                    if (booking.getCancelReason() != null && !booking.getCancelReason().isEmpty()) {
                        body += " Lý do: " + booking.getCancelReason();
                    }

                    notificationService.sendAndSaveNotification(user, title, body, booking.getId(), type);
                }
            }

            // 4. NHẮC NHỞ -> Báo User
            else if (type == NotificationType.REMINDER) {
                User user = booking.getUser();
                if (user != null) {
                    String title = "⚽ Sắp đến giờ ra sân!";
                    String body = "Chỉ còn 15 phút nữa là đến giờ đá tại " + booking.getCourt().getName();

                    notificationService.sendAndSaveNotification(user, title, body, booking.getId(), type);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi xử lý Event trong Listener: " + e.getMessage());
            e.printStackTrace();
        }
    }
}