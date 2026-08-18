package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_rag_chunk")
public class SfRagChunk {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String sourceType;
    private Long sourceId;
    private String sourceKey;
    private String title;
    private String content;
    private OffsetDateTime createdAt;
}
