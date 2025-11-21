package com.example.booking_api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterDeviceTokenRequest {
    private String token;  // FCM token
    private String device; // optional: "Android-Postman", "iPhone 14", ...
}
