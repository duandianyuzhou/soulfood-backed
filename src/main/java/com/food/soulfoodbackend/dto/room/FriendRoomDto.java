package com.food.soulfoodbackend.dto.room;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FriendRoomDto {

    private String code;
    private String topic;
    private String status;
    private Long ownerId;
    private String ownerNickname;
    private int participantCount;
    private String createdAt;
}
