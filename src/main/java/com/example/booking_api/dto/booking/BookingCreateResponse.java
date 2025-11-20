package com.example.booking_api.dto.booking;

import com.example.booking_api.entity.enums.BookingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
public class BookingCreateResponse {
    private UUID id;
    private BigDecimal totalAmount;
    private BookingStatus status;
}
