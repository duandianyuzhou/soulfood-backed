package com.food.soulfoodbackend.dto.room;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class RoomDetailResponse {

    private String code;
    private String topic;
    private String status;
    private Integer maxPeople;
    private Integer durationMin;
    private Integer participantCount;
    private Long myVoteOptionId;
    private List<RoomOptionDto> options;
}
