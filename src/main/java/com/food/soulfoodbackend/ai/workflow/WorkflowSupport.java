package com.food.soulfoodbackend.ai.workflow;

import com.food.soulfoodbackend.ai.stream.ChatStreamEmitter;
import com.food.soulfoodbackend.dto.ai.WorkflowSnapshotDto;
import com.food.soulfoodbackend.dto.ai.WorkflowStepDto;

import java.util.List;
import java.util.UUID;

public final class WorkflowSupport {

    private WorkflowSupport() {
    }

    public static String runId(String prefix) {
        return "w_" + prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    public static void upsertStep(
            ChatStreamEmitter streamEmitter,
            List<String> events,
            WorkflowSnapshotDto snapshot,
            String id,
            String title,
            String status,
            String summary) {
        WorkflowStepDto step = findStep(snapshot, id);
        if (step == null) {
            step = new WorkflowStepDto(id, title, status, summary);
            snapshot.getSteps().add(step);
        } else {
            step.setTitle(title);
            step.setStatus(status);
            step.setSummary(summary);
        }
        events.add(streamEmitter.step(snapshot.getRunId(), id, title, status, summary));
    }

    public static void completeStep(
            ChatStreamEmitter streamEmitter,
            List<String> events,
            WorkflowSnapshotDto snapshot,
            String id,
            String status,
            String summary) {
        WorkflowStepDto step = findStep(snapshot, id);
        String title = step == null ? id : step.getTitle();
        upsertStep(streamEmitter, events, snapshot, id, title, status, summary);
    }

    public static WorkflowStepDto findStep(WorkflowSnapshotDto snapshot, String id) {
        return snapshot.getSteps().stream()
                .filter(item -> id.equals(item.getId()))
                .findFirst()
                .orElse(null);
    }
}
