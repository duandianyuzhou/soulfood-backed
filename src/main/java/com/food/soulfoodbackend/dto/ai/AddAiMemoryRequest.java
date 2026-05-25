package com.food.soulfoodbackend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddAiMemoryRequest {

    @NotBlank
    @Size(max = 512)
    private String content;

    /** long_term | episodic，默认 long_term */
    private String memoryType;
}
