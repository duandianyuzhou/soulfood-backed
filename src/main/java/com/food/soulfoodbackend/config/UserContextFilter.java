package com.food.soulfoodbackend.config;

import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(10)
@RequiredArgsConstructor
public class UserContextFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            resolveUserId(request).ifPresent(UserContext::setUserId);
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    private java.util.Optional<Long> resolveUserId(HttpServletRequest request) {
        String authorization = request.getHeader(HEADER_AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            String token = authorization.substring(BEARER_PREFIX.length()).trim();
            java.util.Optional<Long> fromJwt = jwtService.parseUserId(token);
            if (fromJwt.isPresent()) {
                return fromJwt;
            }
        }

        String rawUserId = request.getHeader(HEADER_USER_ID);
        if (rawUserId != null && !rawUserId.isBlank()) {
            try {
                return java.util.Optional.of(Long.parseLong(rawUserId.trim()));
            } catch (NumberFormatException ignored) {
                return java.util.Optional.empty();
            }
        }
        return java.util.Optional.empty();
    }
}
