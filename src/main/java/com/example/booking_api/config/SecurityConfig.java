package com.example.booking_api.config;
import com.example.booking_api.security.UserRoleFilter;
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
    private final UserRoleFilter userRoleFilter;   // 👈 thêm dòng này

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**", "/actuator/health", "/error").permitAll()
                        .requestMatchers("/v1/admin/**").hasRole("ADMIN")   // giữ nguyên
                        .anyRequest().authenticated()
                )
                // Firebase xác thực token trước
                .addFilterAfter(firebaseAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // Sau đó map UID -> role
                .addFilterAfter(userRoleFilter, FirebaseAuthFilter.class);

        return http.build();
    }
}
