package com.example.booking_api.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UnregisterDeviceTokenRequest {
    private String token; // FCM token cần xóa
}
