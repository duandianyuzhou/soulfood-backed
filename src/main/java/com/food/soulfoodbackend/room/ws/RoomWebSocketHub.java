package com.food.soulfoodbackend.room.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomWebSocketHub {

    private final Map<String, Map<Long, WebSocketSession>> rooms = new ConcurrentHashMap<>();

    public void register(String roomCode, Long userId, WebSocketSession session) {
        session.getAttributes().put("roomCode", roomCode);
        session.getAttributes().put("userId", userId);
        rooms.computeIfAbsent(roomCode, ignored -> new ConcurrentHashMap<>()).put(userId, session);
    }

    public void unregister(WebSocketSession session) {
        String roomCode = (String) session.getAttributes().get("roomCode");
        Long userId = (Long) session.getAttributes().get("userId");
        if (roomCode == null || userId == null) {
            return;
        }
        Map<Long, WebSocketSession> sessions = rooms.get(roomCode);
        if (sessions == null) {
            return;
        }
        sessions.remove(userId, session);
        if (sessions.isEmpty()) {
            rooms.remove(roomCode);
        }
    }

    public Map<Long, WebSocketSession> sessions(String roomCode) {
        Map<Long, WebSocketSession> sessions = rooms.get(roomCode);
        return sessions == null ? Map.of() : Map.copyOf(sessions);
    }
}
