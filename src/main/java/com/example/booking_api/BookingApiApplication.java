package com.example.booking_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling // <--- THÊM DÒNG NÀY
public class BookingApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(BookingApiApplication.class, args);
	}
}
