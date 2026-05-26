package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@TableName("sf_restaurant")
public class SfRestaurant {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String category;
    private String address;
    private BigDecimal lat;
    private BigDecimal lng;
    private BigDecimal rating;
    private BigDecimal distanceKm;
    private String externalId;
    private String phone;
    private String photoUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    @TableLogic
    private Boolean deleted;
}
