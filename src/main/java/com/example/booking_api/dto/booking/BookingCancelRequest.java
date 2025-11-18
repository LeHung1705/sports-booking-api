package com.example.booking_api.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingCancelRequest {

    @JsonProperty("cancel_reason")
    @NotBlank(message = "cancel_reason không được để trống")
    private String cancelReason;
}
