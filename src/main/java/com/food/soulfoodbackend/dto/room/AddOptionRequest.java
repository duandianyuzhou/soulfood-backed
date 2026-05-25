package com.food.soulfoodbackend.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddOptionRequest {

    @NotBlank
    @Size(max = 128)
    private String title;

    private String source = "manual";
}
