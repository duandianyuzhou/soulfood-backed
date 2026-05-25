package com.food.soulfoodbackend.controller;

import com.food.soulfoodbackend.common.ApiResult;
import com.food.soulfoodbackend.dto.recipe.RecipeDetailDto;
import com.food.soulfoodbackend.dto.recipe.RecipeSummaryDto;
import com.food.soulfoodbackend.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    @GetMapping
    public ApiResult<List<RecipeSummaryDto>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return ApiResult.ok(recipeService.list(category, keyword));
    }

    @GetMapping("/{id}")
    public ApiResult<RecipeDetailDto> detail(@PathVariable Long id) {
        return ApiResult.ok(recipeService.getById(id));
    }

    @GetMapping("/by-name")
    public ApiResult<RecipeDetailDto> byName(@RequestParam String name) {
        return ApiResult.ok(recipeService.findByName(name));
    }
}
