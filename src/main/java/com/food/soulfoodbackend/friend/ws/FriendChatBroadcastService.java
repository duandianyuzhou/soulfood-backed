package com.food.soulfoodbackend.friend.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.dto.friend.FriendChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendChatBroadcastService {

    private final FriendChatWebSocketHub hub;
    private final ObjectMapper objectMapper;

    public void broadcastMessage(Long conversationId, FriendChatMessageDto message) {
        Map<Long, WebSocketSession> sessions = hub.sessions(conversationId);
        if (sessions.isEmpty()) {
            return;
        }
        for (WebSocketSession session : sessions.values()) {
            try {
                sendMessageFrame(session, message);
            } catch (IOException ex) {
                log.debug("Skip friend chat WS broadcast conversation={}: {}", conversationId, ex.getMessage());
            }
        }
    }

    public void sendConnected(WebSocketSession session) throws IOException {
        if (session == null || !session.isOpen()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "connected");
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }

    private void sendMessageFrame(WebSocketSession session, FriendChatMessageDto message) throws IOException {
        if (session == null || !session.isOpen()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "message");
        payload.put("data", message);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }
}
