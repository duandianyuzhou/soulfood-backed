package com.food.soulfoodbackend.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class OrdersOverviewResponse {

    private int orderCount;
    private BigDecimal totalAmount;
    private String topCategory;
    private List<OrderDayGroupDto> days;
    /** 是否包含示例订单（非真实消费） */
    private boolean hasDemoOrders;
}
