package com.food.soulfoodbackend.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowSnapshotDto {

    private String runId;
    private String title;
    private String status;
    private List<WorkflowStepDto> steps = new ArrayList<>();
}
