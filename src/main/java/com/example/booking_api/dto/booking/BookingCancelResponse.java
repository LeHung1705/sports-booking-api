package com.example.booking_api.dto.booking;

import com.example.booking_api.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookingCancelResponse {
    private BookingStatus status;
    private BigDecimal refundAmount;
}
