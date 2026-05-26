package com.food.soulfoodbackend.friend.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FriendChatWebSocketHub {

    private final Map<Long, Map<Long, WebSocketSession>> conversations = new ConcurrentHashMap<>();

    public void register(Long conversationId, Long userId, WebSocketSession session) {
        session.getAttributes().put("conversationId", conversationId);
        session.getAttributes().put("userId", userId);
        conversations.computeIfAbsent(conversationId, ignored -> new ConcurrentHashMap<>()).put(userId, session);
    }

    public void unregister(WebSocketSession session) {
        Long conversationId = (Long) session.getAttributes().get("conversationId");
        Long userId = (Long) session.getAttributes().get("userId");
        if (conversationId == null || userId == null) {
            return;
        }
        Map<Long, WebSocketSession> sessions = conversations.get(conversationId);
        if (sessions == null) {
            return;
        }
        sessions.remove(userId, session);
        if (sessions.isEmpty()) {
            conversations.remove(conversationId);
        }
    }

    public Map<Long, WebSocketSession> sessions(Long conversationId) {
        Map<Long, WebSocketSession> sessions = conversations.get(conversationId);
        return sessions == null ? Map.of() : Map.copyOf(sessions);
    }
}
