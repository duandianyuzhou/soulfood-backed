package com.food.soulfoodbackend.friend.ws;

import com.food.soulfoodbackend.service.FriendChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class FriendChatWebSocketHandler extends TextWebSocketHandler {

    private final FriendChatWebSocketHub hub;
    private final FriendChatBroadcastService broadcastService;
    private final FriendChatService friendChatService;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long conversationId = (Long) session.getAttributes().get("conversationId");
        Long userId = (Long) session.getAttributes().get("userId");
        friendChatService.assertCanAccessConversation(userId, conversationId);
        hub.register(conversationId, userId, session);
        broadcastService.sendConnected(session);
        log.debug("Friend chat WS connected conversation={} user={}", conversationId, userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        hub.unregister(session);
        log.debug("Friend chat WS closed conversation={} status={}",
                session.getAttributes().get("conversationId"), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, org.springframework.web.socket.TextMessage message) {
        // 客户端仅接收推送；忽略 ping 文本
    }
}
