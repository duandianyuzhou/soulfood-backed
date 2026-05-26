package com.food.soulfoodbackend.friend.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@RequiredArgsConstructor
public class FriendChatWebSocketConfig implements WebSocketConfigurer {

    private final FriendChatWebSocketHandler friendChatWebSocketHandler;
    private final FriendChatWebSocketAuthInterceptor authInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(friendChatWebSocketHandler, "/ws/friend-chat/*")
                .addInterceptors(authInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
