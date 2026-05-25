package com.food.soulfoodbackend.dto.room;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomResultResponse {

    private Long optionId;
    private String title;
    private Integer voteCount;
    private Integer percent;
}
