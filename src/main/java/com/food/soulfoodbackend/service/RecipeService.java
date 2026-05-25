package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.domain.entity.SfRecipe;
import com.food.soulfoodbackend.dto.recipe.RecipeDetailDto;
import com.food.soulfoodbackend.dto.recipe.RecipeSummaryDto;
import com.food.soulfoodbackend.mapper.SfRecipeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final SfRecipeMapper recipeMapper;

    public List<RecipeSummaryDto> list(String category, String keyword) {
        LambdaQueryWrapper<SfRecipe> wrapper = new LambdaQueryWrapper<SfRecipe>()
                .orderByDesc(SfRecipe::getScore)
                .orderByAsc(SfRecipe::getName);
        if (StringUtils.hasText(category) && !"all".equalsIgnoreCase(category)) {
            wrapper.eq(SfRecipe::getCategory, category);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SfRecipe::getName, keyword)
                    .or()
                    .like(SfRecipe::getCategory, keyword)
                    .or()
                    .like(SfRecipe::getSummary, keyword));
        }
        return recipeMapper.selectList(wrapper).stream().map(this::toSummary).toList();
    }

    public RecipeDetailDto getById(Long id) {
        SfRecipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜谱不存在");
        }
        return toDetail(recipe);
    }

    public RecipeDetailDto findByName(String name) {
        SfRecipe recipe = recipeMapper.selectOne(new LambdaQueryWrapper<SfRecipe>()
                .eq(SfRecipe::getName, name.trim())
                .last("LIMIT 1"));
        if (recipe == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "菜谱不存在");
        }
        return toDetail(recipe);
    }

    private RecipeSummaryDto toSummary(SfRecipe recipe) {
        return new RecipeSummaryDto(
                recipe.getId(),
                recipe.getName(),
                recipe.getCategory(),
                recipe.getDifficulty(),
                recipe.getDurationMin(),
                recipe.getScore(),
                recipe.getSummary());
    }

    private RecipeDetailDto toDetail(SfRecipe recipe) {
        return new RecipeDetailDto(
                recipe.getId(),
                recipe.getName(),
                recipe.getCategory(),
                recipe.getDifficulty(),
                recipe.getDurationMin(),
                recipe.getScore(),
                recipe.getSummary(),
                JsonStrings.parseStringList(recipe.getIngredientsJson()),
                JsonStrings.parseStringList(recipe.getStepsJson()));
    }
}
