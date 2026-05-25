package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_ai_conversation")
public class SfAiConversation {

    @TableId
    private String id;
    private Long userId;
    private String title;
    private String sceneTag;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
