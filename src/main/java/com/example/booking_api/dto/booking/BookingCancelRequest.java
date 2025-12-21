package com.example.booking_api.dto.booking;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class BookingCancelRequest {

    @JsonProperty("cancel_reason")
    @NotBlank(message = "cancel_reason không được để trống")
    private String cancelReason;

    @JsonProperty("bank_name")
    @NotBlank(message = "Tên ngân hàng không được để trống")
    private String bankName;

    @JsonProperty("account_number")
    @NotBlank(message = "Số tài khoản không được để trống")
    private String accountNumber;

    @JsonProperty("account_holder_name")
    @NotBlank(message = "Tên chủ thẻ không được để trống")
    private String accountHolderName;
}
