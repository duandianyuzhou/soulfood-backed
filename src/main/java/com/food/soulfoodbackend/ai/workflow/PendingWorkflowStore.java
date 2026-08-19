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

    public void put(PendingWorkflowSession session) {
        if (session == null || session.getRunId() == null) {
            return;
        }
        cache.put(session.getRunId(), session);
    }

    public PendingWorkflowSession get(String runId) {
        if (runId == null) {
            return null;
        }
        return cache.getIfPresent(runId);
    }

    public void remove(String runId) {
        if (runId != null) {
            cache.invalidate(runId);
        }
    }
}
