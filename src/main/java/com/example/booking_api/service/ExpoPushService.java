package com.example.booking_api.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.*;

@Service
public class ExpoPushService {

    private final String EXPO_API_URL = "https://exp.host/--/api/v2/push/send";

    public void sendExpoNotification(String expoToken, String title, String body) {
        // Kiểm tra token có hợp lệ không (Phải bắt đầu bằng ExponentPushToken)
        if (expoToken == null || !expoToken.startsWith("ExponentPushToken")) {
            System.err.println("❌ Token không phải Expo Token: " + expoToken);
            return;
        }

        RestTemplate restTemplate = new RestTemplate();

        // Tạo body JSON gửi đi
        Map<String, Object> message = new HashMap<>();
        message.put("to", expoToken);
        message.put("title", title);
        message.put("body", body);
        message.put("sound", "default");
        message.put("data", Map.of("someData", "value")); // Dữ liệu kèm theo nếu cần

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Accept", "application/json");

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(message, headers);

        try {
            // Gửi request lên Expo Server
            String response = restTemplate.postForObject(EXPO_API_URL, request, String.class);
            System.out.println("✅ Kết quả gửi Expo: " + response);
        } catch (Exception e) {
            System.err.println("❌ Lỗi gửi Expo: " + e.getMessage());
        }
    }
}