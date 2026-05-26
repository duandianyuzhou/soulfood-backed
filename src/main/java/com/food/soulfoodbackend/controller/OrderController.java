package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.order.CreateOrderRequest;
import com.food.soulfoodbackend.dto.order.CreateVoteOrderRequest;
import com.food.soulfoodbackend.dto.order.OrderItemDto;
import com.food.soulfoodbackend.dto.order.OrdersOverviewResponse;
import com.food.soulfoodbackend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/overview")
    public ApiResult<OrdersOverviewResponse> overview(@RequestParam(required = false) String category) {
        return ApiResult.ok(orderService.overview(UserContext.requireUserId(), category));
    }

    @PostMapping
    public ApiResult<OrderItemDto> create(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResult.ok(orderService.createManualOrder(UserContext.requireUserId(), request));
    }

    @PostMapping("/from-vote")
    public ApiResult<OrderItemDto> createFromVote(@Valid @RequestBody CreateVoteOrderRequest request) {
        return ApiResult.ok(orderService.createVoteOrder(UserContext.requireUserId(), request));
    }
}
