package com.example.booking_api.dto.court;

import com.example.booking_api.entity.enums.SportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CourtRequest {
    @NotBlank
    private String name;

    @NotNull
    private SportType sport;

    @NotNull
    private BigDecimal pricePerHour;
    private Boolean isActive = true;

    @Size(max = 500, message = "URL hình ảnh tối đa 500 ký tự")
//    @Pattern(
//            regexp = "^(https?://[\\w\\-./%?=&]+\\.(png|jpg|jpeg|gif|webp))$",
//            message = "URL hình ảnh không hợp lệ"
//    )
    private String imageUrl;

}
