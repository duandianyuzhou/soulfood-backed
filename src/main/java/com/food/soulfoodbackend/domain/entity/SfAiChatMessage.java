package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_ai_chat_message")
public class SfAiChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String conversationId;
    private Integer sortOrder;
    private String messageType;
    private String content;
    private String metaJson;
    private OffsetDateTime createdAt;
}
