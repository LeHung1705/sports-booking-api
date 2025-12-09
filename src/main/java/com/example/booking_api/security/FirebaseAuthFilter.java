package com.example.booking_api.security;

import com.example.booking_api.entity.enums.UserRole;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class FirebaseAuthFilter extends OncePerRequestFilter {

    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();
        System.out.println("DEBUG: FirebaseAuthFilter processing " + requestPath);

        // ⛔ Bỏ qua các endpoint public
        if (isPublicPath(requestPath)) {
            System.out.println("DEBUG: Skipping public path: " + requestPath);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String token = extractToken(request);
            
            if (token != null) {
                System.out.println("DEBUG: Token found, verifying...");
                FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
                System.out.println("DEBUG: Token verified for UID: " + decodedToken.getUid());

                String uid = decodedToken.getUid();

                // 🔥 Lấy role từ Firebase Custom Claims
                Object claimRole = decodedToken.getClaims().get("role");

                UserRole resolvedRole;
                if (claimRole == null) {
                    resolvedRole = UserRole.USER; // mặc định
                } else {
                    resolvedRole = UserRole.valueOf(claimRole.toString().toUpperCase());
                }

                // Convert to Spring Security Authority → ROLE_OWNER / ROLE_USER / ROLE_ADMIN
                SimpleGrantedAuthority authority =
                        new SimpleGrantedAuthority("ROLE_" + resolvedRole.name());

                // Set authentication vào context
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                uid,
                                null,
                                Collections.singleton(authority)
                        );

                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
                System.out.println("DEBUG: Auth set in context");
            } else {
                System.out.println("DEBUG: No token found in request");
            }

        } catch (Exception e) {
            System.err.println("❌ Token verification failed: " + e.getMessage());
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid or expired token");
            return;
        }
        
        System.out.println("DEBUG: Proceeding down filter chain");
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }
}
