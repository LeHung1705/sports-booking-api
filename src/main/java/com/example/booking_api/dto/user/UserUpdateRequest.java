package com.example.booking_api.dto.user;

import lombok.Data;

@Data
public class UserUpdateRequest {
    private String full_name;
    private String phone;
}
