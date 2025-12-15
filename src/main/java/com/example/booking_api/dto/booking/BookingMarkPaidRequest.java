package com.example.booking_api.dto.booking;

import lombok.Data;

@Data
public class BookingMarkPaidRequest {
    private String refundBankName;
    private String refundAccountNumber;
    private String refundAccountName;
}
