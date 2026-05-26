package com.food.soulfoodbackend.room.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoomWebSocketHandler extends TextWebSocketHandler {

    private final RoomWebSocketHub hub;
    private final RoomWebSocketBroadcastService broadcastService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String roomCode = (String) session.getAttributes().get("roomCode");
        Long userId = (Long) session.getAttributes().get("userId");
        hub.register(roomCode, userId, session);
        broadcastService.sendInitial(session, roomCode, userId);
        log.debug("WS connected room={} user={}", roomCode, userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        hub.unregister(session);
        log.debug("WS closed room={} status={}", session.getAttributes().get("roomCode"), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message) {
        // 客户端无需发消息；忽略 ping 文本
    }
}
