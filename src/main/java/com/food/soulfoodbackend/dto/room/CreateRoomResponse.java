package com.food.soulfoodbackend.dto.room;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateRoomResponse {

    private String code;
    private String topic;
}
