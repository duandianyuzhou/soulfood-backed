package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@TableName("sf_recipe")
public class SfRecipe {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String difficulty;
    private Integer durationMin;
    private BigDecimal score;
    private String summary;
    private String ingredientsJson;
    private String stepsJson;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    @TableLogic
    private Boolean deleted;
}
