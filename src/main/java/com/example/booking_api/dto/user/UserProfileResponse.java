package com.example.booking_api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponse {
    private String id;
    private String full_name;
    private String email;
    private String phone;
    private String role;
}
