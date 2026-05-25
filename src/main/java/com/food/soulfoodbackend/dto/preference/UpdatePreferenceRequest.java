package com.food.soulfoodbackend.dto.preference;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdatePreferenceRequest {

    private List<String> tags;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer spicyLevel;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer sweetLevel;

    private Boolean noCoriander;
    private Boolean noPeanut;
    private List<String> allergens;
}
