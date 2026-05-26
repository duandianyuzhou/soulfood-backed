package com.food.soulfoodbackend.room.ws;

import com.food.soulfoodbackend.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RoomWebSocketAuthInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        String roomCode = extractRoomCode(request.getURI().getPath());
        if (roomCode == null || roomCode.isBlank()) {
            return false;
        }

        String token = null;
        if (request instanceof ServletServerHttpRequest servletRequest) {
            token = servletRequest.getServletRequest().getParameter("token");
        }
        Optional<Long> userId = jwtService.parseUserId(token);
        if (userId.isEmpty()) {
            return false;
        }

        attributes.put("roomCode", roomCode.toUpperCase());
        attributes.put("userId", userId.get());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    static String extractRoomCode(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        int idx = path.lastIndexOf('/');
        if (idx < 0 || idx >= path.length() - 1) {
            return null;
        }
        return path.substring(idx + 1);
    }
}
