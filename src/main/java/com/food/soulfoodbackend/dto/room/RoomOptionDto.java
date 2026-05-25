package com.food.soulfoodbackend.dto.room;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomOptionDto {

    private Long id;
    private String title;
    private Integer voteCount;
    private Integer percent;
    private String source;
}
