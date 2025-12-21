package com.example.booking_api.event.listener;

import com.example.booking_api.entity.User;
import com.example.booking_api.entity.enums.NotificationType;
import com.example.booking_api.event.BookingEvent;
import com.example.booking_api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class BookingListener {

    private final NotificationService notificationService;

    @Async // Chạy ngầm để không block luồng chính
    @EventListener
    public void handleBookingEvent(BookingEvent event) {
        try {
            var booking = event.getBooking();
            var type = event.getType();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm dd/MM");

            // -----------------------------------------------------------------
            // TRƯỜNG HỢP 1: CÓ ĐƠN ĐẶT MỚI -> Báo cho CHỦ SÂN (Owner)
            // -----------------------------------------------------------------
            if (type == NotificationType.BOOKING_CREATED) {
                User owner = booking.getCourt().getVenue().getOwner();
                if (owner != null) {
                    String title = "🔔 Có đơn đặt sân mới!";
                    String body = "Khách hàng " + booking.getUser().getFullName()
                            + " vừa đặt sân " + booking.getCourt().getName()
                            + " lúc " + booking.getStartTime().format(formatter);

                    // Gọi hàm Service để: Lưu DB + Bắn Push
                    notificationService.sendAndSaveNotification(owner, title, body, booking.getId(), type);
                    System.out.println("Listener: Đã báo đơn mới cho Owner " + owner.getEmail());
                }
            }

            // -----------------------------------------------------------------
            // TRƯỜNG HỢP 2: ĐÃ XÁC NHẬN -> Báo cho KHÁCH (User)
            // -----------------------------------------------------------------
            else if (type == NotificationType.BOOKING_CONFIRMED) {
                User user = booking.getUser();
                if (user != null) {
                    String title = "✅ Đặt sân thành công!";
                    String body = "Chủ sân đã xác nhận lịch đá tại " + booking.getCourt().getName()
                            + " (" + booking.getStartTime().format(formatter) + ")";

                    notificationService.sendAndSaveNotification(user, title, body, booking.getId(), type);
                    System.out.println("Listener: Đã báo thành công cho User " + user.getEmail());
                }
            }

            // -----------------------------------------------------------------
            // TRƯỜNG HỢP 3: NHẮC NHỞ -> Báo cho KHÁCH (User)
            // -----------------------------------------------------------------
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
        }
    }
}