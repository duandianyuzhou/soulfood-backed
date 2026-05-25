package com.food.soulfoodbackend.dto.recipe;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
public class RecipeDetailDto {

    private Long id;
    private String name;
    private String category;
    private String difficulty;
    private Integer durationMin;
    private BigDecimal score;
    private String summary;
    private List<String> ingredients;
    private List<String> steps;
}
