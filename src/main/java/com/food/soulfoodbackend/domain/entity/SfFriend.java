package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_friend")
public class SfFriend {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long friendUserId;
    private String source;
    private OffsetDateTime createdAt;
}
