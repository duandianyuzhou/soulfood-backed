package com.food.soulfoodbackend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowStepDto {

    private String id;
    private String title;
    private String status;
    private String summary;
}
