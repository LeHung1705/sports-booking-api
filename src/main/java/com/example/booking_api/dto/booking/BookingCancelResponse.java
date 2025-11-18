package com.example.booking_api.dto.booking;

import com.example.booking_api.entity.enums.BookingStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BookingCancelResponse {
    private BookingStatus status;
}
