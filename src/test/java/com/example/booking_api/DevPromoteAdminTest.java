package com.example.booking_api;

import com.example.booking_api.entity.enums.UserRole;
import com.example.booking_api.service.AdminService;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

@SpringBootTest
class DevPromoteAdminTest {

    // Thay UID này bằng firebase_uid của tài khoản bạn đang dùng để test
    private static final String UID = "ZXjjk69ILubLmdRzCUtEzwN8RMD3";

    @BeforeAll
    static void initFirebase() throws Exception {
        if (FirebaseApp.getApps().isEmpty()) {
            // Nếu bạn dùng Cách A đặt file trong test/resources:
            try (InputStream in = new ClassPathResource("firebase-service-account.json").getInputStream()) {
                FirebaseOptions opts = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(in))
                        .build();
                FirebaseApp.initializeApp(opts);
            }
            // Nếu bạn dùng Cách B (env var), đoạn trên vẫn chạy OK vì GoogleCredentials
            // cũng tự đọc từ env khi không truyền stream. (Giữ như trên là an toàn.)
        }
    }

    @Autowired
    private AdminService adminService;

    @Test
    void promoteMeToAdmin() throws Exception {
        adminService.updateUserRole(UID, UserRole.ADMIN);
        System.out.println("✅ Set custom claim role=ADMIN cho UID: " + UID);
    }
}