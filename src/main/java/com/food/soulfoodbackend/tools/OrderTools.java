package com.food.soulfoodbackend.tools;

import com.food.soulfoodbackend.domain.entity.SfOrder;
import com.food.soulfoodbackend.mapper.SfOrderMapper;
import com.food.soulfoodbackend.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 订单tools
 * @author: tao yi
 * @date: 2026/6/1 16:07
 * @version: 1.0
 */
@Component
@RequiredArgsConstructor
public class OrderTools {

    private final SfOrderMapper sfOrderMapper;

    @Tool
    public SfOrder queryOrder(Long id){
        //订单查询
        return sfOrderMapper.selectById(id);
    }

}
