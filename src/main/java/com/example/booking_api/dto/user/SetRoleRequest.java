package com.example.booking_api.dto.user;

import com.example.booking_api.entity.enums.UserRole;
import lombok.Data;

@Data
public class SetRoleRequest {
    private UserRole role;
}
