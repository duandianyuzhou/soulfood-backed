package com.food.soulfoodbackend.ai.workflow;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.domain.entity.SfAiWorkflowPending;
import com.food.soulfoodbackend.mapper.SfAiWorkflowPendingMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingWorkflowStore {

    private static final Duration TTL = Duration.ofDays(7);

    private final SfAiWorkflowPendingMapper mapper;
    private final ObjectMapper objectMapper;
    private final Cache<String, PendingWorkflowSession> cache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .maximumSize(2000)
            .build();

    public void put(PendingWorkflowSession session) {
        if (session == null || session.getRunId() == null) {
            return;
        }
        cache.put(session.getRunId(), session);
        try {
            if (session.getConversationId() != null) {
                mapper.delete(new LambdaQueryWrapper<SfAiWorkflowPending>()
                        .eq(SfAiWorkflowPending::getConversationId, session.getConversationId())
                        .ne(SfAiWorkflowPending::getRunId, session.getRunId()));
            }
            SfAiWorkflowPending row = new SfAiWorkflowPending();
            row.setRunId(session.getRunId());
            row.setConversationId(session.getConversationId());
            row.setUserId(session.getUserId());
            row.setKind(session.getKind() == null ? null : session.getKind().name());
            row.setWaitingStepId(session.getWaitingStepId());
            row.setPayloadJson(objectMapper.writeValueAsString(session));
            row.setExpiresAt(OffsetDateTime.now().plus(TTL));
            row.setCreatedAt(OffsetDateTime.now());
            mapper.deleteById(session.getRunId());
            mapper.insert(row);
        } catch (Exception ex) {
            log.warn("persist pending workflow failed: {}", ex.getMessage());
        }
    }

    public PendingWorkflowSession get(String runId) {
        if (runId == null) {
            return null;
        }
        PendingWorkflowSession cached = cache.getIfPresent(runId);
        if (cached != null) {
            return cached;
        }
        return load(mapper.selectById(runId));
    }

    public PendingWorkflowSession getByConversation(String conversationId) {
        if (conversationId == null) {
            return null;
        }
        SfAiWorkflowPending row = mapper.selectOne(new LambdaQueryWrapper<SfAiWorkflowPending>()
                .eq(SfAiWorkflowPending::getConversationId, conversationId)
                .gt(SfAiWorkflowPending::getExpiresAt, OffsetDateTime.now())
                .orderByDesc(SfAiWorkflowPending::getCreatedAt)
                .last("LIMIT 1"));
        PendingWorkflowSession session = load(row);
        if (session != null) {
            cache.put(session.getRunId(), session);
        }
        return session;
    }

    public void remove(String runId) {
        if (runId == null) {
            return;
        }
        cache.invalidate(runId);
        mapper.deleteById(runId);
    }

    private PendingWorkflowSession load(SfAiWorkflowPending row) {
        if (row == null) {
            return null;
        }
        if (row.getExpiresAt() != null && row.getExpiresAt().isBefore(OffsetDateTime.now())) {
            mapper.deleteById(row.getRunId());
            return null;
        }
        try {
            PendingWorkflowSession session = objectMapper.readValue(row.getPayloadJson(), PendingWorkflowSession.class);
            cache.put(session.getRunId(), session);
            return session;
        } catch (Exception ex) {
            log.warn("load pending workflow failed: {}", ex.getMessage());
            return null;
        }
    }
}
