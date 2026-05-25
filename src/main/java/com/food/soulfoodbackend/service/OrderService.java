package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.domain.entity.SfOrder;
import com.food.soulfoodbackend.dto.order.OrderDayGroupDto;
import com.food.soulfoodbackend.dto.order.OrderItemDto;
import com.food.soulfoodbackend.dto.order.OrdersOverviewResponse;
import com.food.soulfoodbackend.mapper.SfOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final DateTimeFormatter DAY_LABEL = DateTimeFormatter.ofPattern("yyyy-MM-dd EEEE", Locale.CHINA);
    private static final DateTimeFormatter TIME_TEXT = DateTimeFormatter.ofPattern("HH:mm");

    private final SfOrderMapper orderMapper;

    public OrdersOverviewResponse overview(Long userId, String category) {
        LambdaQueryWrapper<SfOrder> wrapper = new LambdaQueryWrapper<SfOrder>()
                .eq(SfOrder::getUserId, userId)
                .orderByDesc(SfOrder::getOrderedAt);
        if (category != null && !category.isBlank() && !"all".equalsIgnoreCase(category)) {
            wrapper.eq(SfOrder::getCategory, category);
        }

        List<SfOrder> rows = orderMapper.selectList(wrapper);
        if (rows.isEmpty()) {
            seedDemoOrders(userId);
            rows = orderMapper.selectList(wrapper);
        }

        BigDecimal total = rows.stream()
                .map(SfOrder::getAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String topCategory = rows.stream()
                .collect(Collectors.groupingBy(r -> r.getCategory() == null ? "其他" : r.getCategory(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("—");

        Map<String, List<SfOrder>> grouped = rows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getOrderedAt().toLocalDate().toString(),
                        LinkedHashMap::new,
                        Collectors.toList()));

        List<OrderDayGroupDto> days = new ArrayList<>();
        grouped.forEach((day, items) -> {
            items.sort(Comparator.comparing(SfOrder::getOrderedAt).reversed());
            String dateLabel = items.get(0).getOrderedAt().format(DAY_LABEL);
            String dayCategory = items.get(0).getCategory();
            List<OrderItemDto> orderItems = items.stream().map(this::toItem).toList();
            days.add(new OrderDayGroupDto(dateLabel, dayCategory, orderItems));
        });

        return new OrdersOverviewResponse(rows.size(), total, topCategory, days);
    }

    private OrderItemDto toItem(SfOrder row) {
        String status = "已完成";
        if (row.getItemSummary() != null && row.getItemSummary().contains("发票")) {
            status = "待开发票";
        } else if (row.getItemSummary() != null && row.getItemSummary().contains("评价")) {
            status = "已评价";
        }
        return new OrderItemDto(
                row.getId(),
                row.getRestaurantName(),
                row.getCategory(),
                row.getAmount(),
                row.getItemSummary(),
                row.getOrderedAt() == null ? "" : TIME_TEXT.format(row.getOrderedAt()),
                status);
    }

    private void seedDemoOrders(Long userId) {
        long count = orderMapper.selectCount(new LambdaQueryWrapper<SfOrder>().eq(SfOrder::getUserId, userId));
        if (count > 0) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        insertDemo(userId, "海底捞（万象城店）", "food", new BigDecimal("168.00"),
                "2人 · 18:42 · 线下堂食", now.minusDays(15));
        insertDemo(userId, "喜茶（万象城店）", "drink", new BigDecimal("38.00"),
                "2杯 · 15:20 · 外带 · 已评价", now.minusDays(17));
        insertDemo(userId, "麦当劳（万象城店）", "fast", new BigDecimal("46.00"),
                "1人 · 12:05 · 自取 · 待开发票", now.minusDays(19));
    }

    private void insertDemo(Long userId, String name, String category, BigDecimal amount, String summary, OffsetDateTime orderedAt) {
        SfOrder row = new SfOrder();
        row.setUserId(userId);
        row.setRestaurantName(name);
        row.setCategory(category);
        row.setAmount(amount);
        row.setItemSummary(summary);
        row.setOrderedAt(orderedAt);
        row.setCreatedAt(OffsetDateTime.now());
        row.setDeleted(false);
        orderMapper.insert(row);
    }
}
