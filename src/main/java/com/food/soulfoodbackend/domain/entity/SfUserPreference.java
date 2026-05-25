package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_user_preference")
public class SfUserPreference {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String tagsJson;
    private Integer spicyLevel;
    private Integer sweetLevel;
    private Boolean noCoriander;
    private Boolean noPeanut;
    private String allergensJson;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
