package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_ai_user_memory")
public class SfAiUserMemory {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    /** long_term | episodic */
    private String memoryType;
    private String content;
    /** auto | user | conversation | activity */
    private String source;
    private String sourceRef;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
