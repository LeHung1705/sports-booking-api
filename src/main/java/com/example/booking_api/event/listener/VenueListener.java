package com.example.booking_api.event.listener;

import com.example.booking_api.entity.User;
import com.example.booking_api.entity.Venue;
import com.example.booking_api.entity.enums.NotificationType;
import com.example.booking_api.entity.enums.UserRole;
import com.example.booking_api.event.VenueEvent;
import com.example.booking_api.repository.UserRepository;
import com.example.booking_api.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
public class VenueListener {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleVenueEvent(VenueEvent event) {
        System.out.println("👂 [VenueListener] Received event: " + event.getType());
        try {
            Venue venue = event.getVenue();
            NotificationType type = event.getType();

            // 1. VENUE CREATED -> Báo cho ADMIN
            if (type == NotificationType.VENUE_CREATED) {
                System.out.println("Processing VENUE_CREATED...");
                List<User> admins = userRepository.findByRole(UserRole.ADMIN);
                System.out.println("Found " + admins.size() + " admins.");
                for (User admin : admins) {
                    String title = "🏟️ Yêu cầu phê duyệt địa điểm mới";
                    String body = "Owner " + venue.getOwner().getFullName() + " vừa tạo địa điểm: " + venue.getName();
                    notificationService.sendAndSaveNotification(admin, title, body, null, venue.getId(), type);
                }
            }

            // 2. VENUE APPROVED -> Báo cho OWNER
            else if (type == NotificationType.VENUE_APPROVED) {
                System.out.println("Processing VENUE_APPROVED...");
                User owner = venue.getOwner();
                if (owner != null) {
                    String title = "✅ Địa điểm của bạn đã được duyệt!";
                    String body = "Admin đã phê duyệt địa điểm: " + venue.getName() + ". Bạn có thể bắt đầu kinh doanh ngay.";
                    notificationService.sendAndSaveNotification(owner, title, body, null, venue.getId(), type);
                }
            }
            
             // 3. VENUE REJECTED -> Báo cho OWNER
            else if (type == NotificationType.VENUE_REJECTED) {
                 System.out.println("Processing VENUE_REJECTED...");
                User owner = venue.getOwner();
                if (owner != null) {
                    String title = "❌ Địa điểm bị từ chối";
                    String body = "Địa điểm: " + venue.getName() + " đã bị từ chối phê duyệt. Vui lòng kiểm tra lại thông tin.";
                    notificationService.sendAndSaveNotification(owner, title, body, null, venue.getId(), type);
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Lỗi xử lý Venue Event: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
