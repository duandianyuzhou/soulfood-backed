package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_friend_conversation")
public class SfFriendConversation {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userLowId;
    private Long userHighId;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
