package com.example.booking_api.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class LoginResponse {
    public String accessToken;
    public Object user;
}
