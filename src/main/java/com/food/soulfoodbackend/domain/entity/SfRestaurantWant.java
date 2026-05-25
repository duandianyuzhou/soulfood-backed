package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_restaurant_want")
public class SfRestaurantWant {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long restaurantId;
    private OffsetDateTime createdAt;
}
