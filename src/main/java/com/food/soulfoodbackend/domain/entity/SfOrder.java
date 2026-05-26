package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@TableName("sf_order")
public class SfOrder {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String restaurantName;
    private String category;
    private BigDecimal amount;
    private String itemSummary;
    private OffsetDateTime orderedAt;
    private OffsetDateTime createdAt;
    /** manual 用户记账 / demo 示例数据 */
    private String source;
    @TableLogic
    private Boolean deleted;
}
