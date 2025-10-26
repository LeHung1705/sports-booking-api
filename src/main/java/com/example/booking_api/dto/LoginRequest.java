package com.example.booking_api.dto;

import lombok.Data;

@Data
public class LoginRequest {
    public String email;
    private String password;
}
