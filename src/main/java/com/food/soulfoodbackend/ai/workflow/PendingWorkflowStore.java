package com.food.soulfoodbackend.ai.workflow;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PendingWorkflowStore {

    private final Cache<String, PendingWorkflowSession> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(2000)
            .build();
    private final Cache<String, String> byConversation = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(2000)
            .build();

    public void put(PendingWorkflowSession session) {
        if (session == null || session.getRunId() == null) {
            return;
        }
        cache.put(session.getRunId(), session);
        if (session.getConversationId() != null) {
            byConversation.put(session.getConversationId(), session.getRunId());
        }
    }

    public PendingWorkflowSession get(String runId) {
        if (runId == null) {
            return null;
        }
        return cache.getIfPresent(runId);
    }

    public PendingWorkflowSession getByConversation(String conversationId) {
        if (conversationId == null) {
            return null;
        }
        String runId = byConversation.getIfPresent(conversationId);
        return runId == null ? null : cache.getIfPresent(runId);
    }

    public void remove(String runId) {
        if (runId == null) {
            return;
        }
        PendingWorkflowSession session = cache.getIfPresent(runId);
        cache.invalidate(runId);
        if (session != null && session.getConversationId() != null) {
            byConversation.invalidate(session.getConversationId());
        }
    }
}
