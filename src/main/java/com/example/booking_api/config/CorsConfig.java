package com.example.booking_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ Dùng allowedOriginPatterns thay vì allowedOrigins khi dùng allowCredentials
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:*",
                "http://192.168.*:*"
        ));

        // Hoặc nếu muốn cụ thể hơn:
        // configuration.setAllowedOrigins(Arrays.asList(
        //     "http://localhost:8081",
        //     "http://192.168.56.1:8081"
        // ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"
        ));

        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}