package com.example.booking_api.dto.court;

import com.example.booking_api.entity.enums.SportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourtRequest {
    @NotBlank
    private String name;

    @NotNull
    private SportType sport;

    @NotNull
    private Double pricePerHour;
    private Boolean isActive = true;
}
