package com.food.soulfoodbackend.room.ws;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RoomWebSocketEventListener {

    private final RoomWebSocketBroadcastService broadcastService;

    @EventListener
    public void onRoomUpdated(RoomUpdatedEvent event) {
        broadcastService.broadcast(event.roomCode());
    }
}
