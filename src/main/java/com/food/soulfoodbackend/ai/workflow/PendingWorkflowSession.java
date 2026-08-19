package com.food.soulfoodbackend.ai.workflow;

import com.food.soulfoodbackend.ai.router.ChatIntent;
import com.food.soulfoodbackend.dto.ai.WorkflowSnapshotDto;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class PendingWorkflowSession {

    private String runId;
    private ChatIntent kind;
    private String conversationId;
    private Long userId;
    private String waitingStepId;
    private String userText;
    private String topic;
    private List<String> draftOptions = new ArrayList<>();
    private Double lat;
    private Double lng;
    private WorkflowSnapshotDto snapshot;
}
