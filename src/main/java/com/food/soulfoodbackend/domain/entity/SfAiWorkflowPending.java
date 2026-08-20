package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_ai_workflow_pending")
public class SfAiWorkflowPending {

    @TableId(type = IdType.INPUT)
    private String runId;
    private String conversationId;
    private Long userId;
    private String kind;
    private String waitingStepId;
    private String payloadJson;
    private OffsetDateTime expiresAt;
    private OffsetDateTime createdAt;
}
