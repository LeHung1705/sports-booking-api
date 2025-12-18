package com.example.booking_api.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserProfileResponse {
    private String id;
    private String full_name;
    private String email;
    private String phone;
    private String role;
    private String avatar;
    private Map<String, Object> stats;
}

