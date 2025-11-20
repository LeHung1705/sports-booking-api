package com.example.booking_api.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource; // 1. Import ClassPathResource

import java.io.InputStream; // 2. Import InputStream

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void init() {
        try {
            // 3. Sử dụng ClassPathResource để lấy file từ classpath
            ClassPathResource resource = new ClassPathResource("firebase-service-account.json");

            // 4. Lấy file dưới dạng InputStream
            InputStream serviceAccount = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount)) // 5. Đọc từ stream
                    .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
            }

            System.out.println("✅ Firebase initialized successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Failed to initialize Firebase: " + e.getMessage());
        }
    }
}