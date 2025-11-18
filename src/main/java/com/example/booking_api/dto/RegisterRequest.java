package com.example.booking_api.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String full_name;
    private String email;
    private String phone;
    private String password;
}
