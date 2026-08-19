package com.food.soulfoodbackend.ai.react;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ToolCallGuard {

    public static final int MAX_TOOL_CALLS = 5;

    private static final ThreadLocal<State> HOLDER = new ThreadLocal<>();

    public void begin(String conversationId) {
        State state = new State();
        state.conversationId = conversationId == null ? "" : conversationId;
        HOLDER.set(state);
    }

    public void end() {
        HOLDER.remove();
    }

    public boolean tryAcquire(String toolName) {
        State state = HOLDER.get();
        if (state == null) {
            return true;
        }
        state.count++;
        if (state.count > MAX_TOOL_CALLS) {
            log.warn("tool limit exceeded conv={} tool={} count={}", state.conversationId, toolName, state.count);
            return false;
        }
        return true;
    }

    public void logFinish(String toolName, boolean success, long elapsedMs) {
        State state = HOLDER.get();
        String conv = state == null ? "-" : state.conversationId;
        log.info("tool conv={} name={} ok={} ms={}", conv, toolName, success, elapsedMs);
    }

    private static final class State {
        private String conversationId;
        private int count;
    }
}
