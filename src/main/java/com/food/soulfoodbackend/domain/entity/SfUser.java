package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_user")
public class SfUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String openId;
    private String nickname;
    private String avatarUrl;
    private String signature;
    private String phone;
    private Integer status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    @TableLogic
    private Boolean deleted;
}
