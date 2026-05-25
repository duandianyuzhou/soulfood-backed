package com.food.soulfoodbackend.config;

import com.food.soulfoodbackend.common.UserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(10)
public class UserContextFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String raw = request.getHeader(HEADER_USER_ID);
            if (raw != null && !raw.isBlank()) {
                UserContext.setUserId(Long.parseLong(raw.trim()));
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
