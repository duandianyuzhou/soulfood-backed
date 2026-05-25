package com.food.soulfoodbackend.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class OrderItemDto {

    private Long id;
    private String restaurantName;
    private String category;
    private BigDecimal amount;
    private String itemSummary;
    private String timeText;
    private String statusText;
}
