package com.food.soulfoodbackend.ai.workflow;

import com.food.soulfoodbackend.ai.router.ChatIntent;
import com.food.soulfoodbackend.dto.ai.WorkflowContinueRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WorkflowRegistry {

    private final NearbyEatWorkflow nearbyEatWorkflow;
    private final CookFromFridgeWorkflow cookFromFridgeWorkflow;
    private final VoteRoomWorkflow voteRoomWorkflow;

    public WorkflowResult start(
            ChatIntent intent,
            String conversationId,
            String userText,
            Long userId,
            Double lat,
            Double lng,
            String imageBase64,
            String imageMimeType) {
        return switch (intent) {
            case NEARBY_EAT -> nearbyEatWorkflow.run(conversationId, userText, userId, lat, lng);
            case COOK_FROM_FRIDGE -> cookFromFridgeWorkflow.start(
                    conversationId, userText, userId, imageBase64, imageMimeType);
            case VOTE_ROOM -> voteRoomWorkflow.start(conversationId, userText, userId, lat, lng);
            default -> throw new IllegalArgumentException("unsupported workflow: " + intent);
        };
    }

    public WorkflowResult resume(PendingWorkflowSession session, WorkflowContinueRequest request) {
        if (session.getKind() == ChatIntent.NEARBY_EAT) {
            return nearbyEatWorkflow.resume(session, request.getLat(), request.getLng(), request.getMessage());
        }
        if (session.getKind() == ChatIntent.COOK_FROM_FRIDGE) {
            return cookFromFridgeWorkflow.resume(
                    session, request.getImageBase64(), request.getImageMimeType(), request.getMessage());
        }
        if (session.getKind() == ChatIntent.VOTE_ROOM) {
            return voteRoomWorkflow.resume(session, request.getOptions(), request.getMessage());
        }
        throw new IllegalArgumentException("unsupported workflow: " + session.getKind());
    }
}
