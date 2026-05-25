package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.common.UserContext;
import com.food.soulfoodbackend.dto.restaurant.RestaurantDto;
import com.food.soulfoodbackend.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService restaurantService;

    @GetMapping("/nearby")
    public ApiResult<List<RestaurantDto>> nearby(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(restaurantService.listNearby(UserContext.getUserId(), category, keyword));
    }

    @PostMapping("/{id}/want")
    public ApiResult<Boolean> want(@PathVariable Long id) {
        restaurantService.markWant(UserContext.requireUserId(), id);
        return ApiResult.ok(Boolean.TRUE);
    }
}
