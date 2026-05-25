package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_vote")
public class SfVote {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long optionId;
    private Long userId;
    private OffsetDateTime createdAt;
}
