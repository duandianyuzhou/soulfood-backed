package com.food.soulfoodbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.soulfoodbackend.domain.entity.SfAiChatMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SfAiChatMessageMapper extends BaseMapper<SfAiChatMessage> {
}
