package com.example.booking_api.config;

import com.example.booking_api.security.FirebaseAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final FirebaseAuthFilter firebaseAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Bật Basic Auth để bạn có thể test nhanh (không ảnh hưởng Bearer)
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints
                        .requestMatchers("/api/v1/auth/**", "/actuator/health", "/error").permitAll()

                        // Khu vực admin (nếu cần test admin sau thì phải gán ROLE_ADMIN từ filter/claims)
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")

                        // Còn lại yêu cầu đã đăng nhập (Bearer Firebase)
                        .anyRequest().authenticated()
                )
                // Xác thực Firebase token sau UsernamePasswordAuthenticationFilter
                .addFilterAfter(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
