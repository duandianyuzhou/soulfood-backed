package com.food.soulfoodbackend.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OrderDayGroupDto {

    private String dateLabel;
    private String category;
    private List<OrderItemDto> items;
}
