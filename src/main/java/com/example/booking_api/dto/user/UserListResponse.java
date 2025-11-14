package com.example.booking_api.dto.user;

import com.example.booking_api.entity.enums.UserRole;
import lombok.Data;

@Data
public class UserListResponse {
    private String uid;
    private String email;
    private String fullName;
    private UserRole role;
}
