package com.food.soulfoodbackend.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@TableName("sf_room_option")
public class SfRoomOption {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private String title;
    private Integer voteCount;
    private String source;
    private Integer sortOrder;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    @TableLogic
    private Boolean deleted;
}
