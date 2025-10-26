package com.example.booking_api.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String full_name;
    private String phone;
}
