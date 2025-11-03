package com.example.booking_api.dto;
import lombok.AllArgsConstructor; //Tự động sinh ra các constructor
import lombok.Data; //tự động sinh các getter setter toString

@Data
@AllArgsConstructor
public class ForgotPasswordResponse {
    private String message;
}
