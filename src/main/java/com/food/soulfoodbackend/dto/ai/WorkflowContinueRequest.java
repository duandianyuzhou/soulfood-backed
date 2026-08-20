package com.food.soulfoodbackend.dto.ai;

import lombok.Data;

import java.util.List;

@Data
public class WorkflowContinueRequest {

    private String conversationId;
    private String stepId;
    private String message;
    private List<String> options;
    private String imageBase64;
    private String imageMimeType;
    private Double lat;
    private Double lng;
}
