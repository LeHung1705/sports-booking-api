package com.example.booking_api.security;

import com.example.booking_api.entity.User;
import com.example.booking_api.entity.enums.UserRole;
import com.example.booking_api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class UserRoleFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        var context = SecurityContextHolder.getContext();
        var auth = context.getAuthentication();

        // Chỉ xử lý nếu FirebaseAuthFilter đã set auth và principal là UID (String)
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof String uid) {

            // Lấy User theo firebaseUid
            User user = userRepository.findByFirebaseUid(uid).orElse(null);

            if (user != null) {
                List<GrantedAuthority> authorities = new ArrayList<>();

                // Ai cũng có ROLE_USER
                authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

                if (user.getRole() == UserRole.OWNER) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_OWNER"));
                }
                if (user.getRole() == UserRole.ADMIN) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                }

                // Giữ nguyên principal = uid để Notification vẫn hoạt động bình thường
                UsernamePasswordAuthenticationToken newAuth =
                        new UsernamePasswordAuthenticationToken(uid, auth.getCredentials(), authorities);
                newAuth.setDetails(auth.getDetails());

                context.setAuthentication(newAuth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
