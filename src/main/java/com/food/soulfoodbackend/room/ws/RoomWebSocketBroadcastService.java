package com.food.soulfoodbackend.room.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.dto.room.RoomDetailResponse;
import com.food.soulfoodbackend.service.RoomService;
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
public class RoomWebSocketBroadcastService {

    private final RoomWebSocketHub hub;
    private final RoomService roomService;
    private final ObjectMapper objectMapper;

    public void broadcast(String roomCode) {
        Map<Long, WebSocketSession> sessions = hub.sessions(roomCode);
        if (sessions.isEmpty()) {
            return;
        }
        for (Map.Entry<Long, WebSocketSession> entry : sessions.entrySet()) {
            try {
                RoomDetailResponse detail = roomService.getRoomDetail(roomCode, entry.getKey());
                send(entry.getValue(), detail);
            } catch (Exception ex) {
                log.debug("Skip WS broadcast for room {} user {}: {}", roomCode, entry.getKey(), ex.getMessage());
            }
        }
    }

    public void sendInitial(WebSocketSession session, String roomCode, Long userId) throws IOException {
        RoomDetailResponse detail = roomService.getRoomDetail(roomCode, userId);
        send(session, detail);
    }

    private void send(WebSocketSession session, RoomDetailResponse detail) throws IOException {
        if (session == null || !session.isOpen()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("type", "room_update");
        payload.put("data", detail);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
    }
}
