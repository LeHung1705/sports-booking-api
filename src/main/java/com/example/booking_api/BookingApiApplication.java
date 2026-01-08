package com.example.booking_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableScheduling // 👈 1. QUAN TRỌNG: Dòng này để bật tính năng lên lịch
@EnableAsync // 👈 BẬT TÍNH NĂNG ASYNC CHO LISTENER
public class BookingApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(BookingApiApplication.class, args);
	}
	// 👇 2. QUAN TRỌNG: Tạo Bean TaskScheduler để BookingService sử dụng
	@Bean
	public TaskScheduler taskScheduler() {
		ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
		scheduler.setPoolSize(5); // Cho phép chạy song song 5 luồng (để không bị tắc nghẽn)
		scheduler.setThreadNamePrefix("booking-scheduler-");
		scheduler.initialize();
		return scheduler;
	}
}
