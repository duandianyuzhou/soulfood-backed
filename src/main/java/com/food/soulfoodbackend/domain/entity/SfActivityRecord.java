package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_activity_record")
public class SfActivityRecord {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String recordType;
    private String title;
    private String summary;
    private Long refId;
    private String extraJson;
    private OffsetDateTime occurredAt;
    private OffsetDateTime createdAt;
}
