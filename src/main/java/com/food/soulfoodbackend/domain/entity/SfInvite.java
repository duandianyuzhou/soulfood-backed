package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_invite")
public class SfInvite {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long inviterId;
    private String inviteCode;
    private Long inviteeId;
    private String status;
    private String rewardStatus;
    private OffsetDateTime createdAt;
    private OffsetDateTime completedAt;
}
