package com.example.booking_api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {
    private String id;
    private String email;
    private String full_name;
    private String token;
}
