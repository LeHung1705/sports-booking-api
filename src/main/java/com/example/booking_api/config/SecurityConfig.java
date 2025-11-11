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

                // 🔐 Bật Basic Auth để test bằng Postman
                .httpBasic(Customizer.withDefaults())

                .authorizeHttpRequests(auth -> auth
                        // Cho phép các endpoint public
                        .requestMatchers("/api/v1/auth/**", "/actuator/health", "/error").permitAll()

                        // Admin phải có ROLE_ADMIN
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")

                        // còn lại chỉ cần đã xác thực
                        .anyRequest().authenticated()
                )

                // 🧩 Đặt Firebase filter CHẠY SAU BasicAuth; nếu không có Bearer thì filter phải bỏ qua.
                .addFilterAfter(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
