package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_room")
public class SfRoom {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String topic;
    private Integer maxPeople;
    private Integer durationMin;
    private String status;
    private Long ownerId;
    private Long winnerOptionId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime closedAt;
    @TableLogic
    private Boolean deleted;
}
