package com.food.soulfoodbackend.ai.react;

import com.food.soulfoodbackend.config.AiRagProperties;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Component
@Slf4j
public class ToolCallGuard {

    public static final String LIMIT_JSON = "{\"error\":\"工具调用次数已达上限，请根据已有结果直接回答用户，不要再调用工具\"}";
    public static final String TIMEOUT_JSON = "{\"error\":\"工具调用超时，请根据已有信息直接回答，不要再调用该工具\"}";

    private static final ThreadLocal<State> HOLDER = new ThreadLocal<>();

    private final AiRagProperties properties;
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "ai-tool-call");
        thread.setDaemon(true);
        return thread;
    });

    public ToolCallGuard(AiRagProperties properties) {
        this.properties = properties;
    }

    public int maxCalls() {
        return Math.max(1, properties.getToolMaxCalls());
    }

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
        if (state.count > maxCalls()) {
            log.warn("tool limit exceeded conv={} tool={} count={}", state.conversationId, toolName, state.count);
            return false;
        }
        return true;
    }

    public String execute(String toolName, Supplier<String> body) {
        if (!tryAcquire(toolName)) {
            return LIMIT_JSON;
        }
        long started = System.nanoTime();
        boolean ok = true;
        try {
            int timeoutMs = properties.getToolTimeoutMs() > 0 ? properties.getToolTimeoutMs() : 8000;
            return CompletableFuture.supplyAsync(body, executor)
                    .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .join();
        } catch (CompletionException ex) {
            ok = false;
            Throwable cause = ex.getCause() == null ? ex : ex.getCause();
            if (cause instanceof TimeoutException) {
                log.warn("tool timeout conv={} tool={} ms={}", conversationId(), toolName, properties.getToolTimeoutMs());
                return TIMEOUT_JSON;
            }
            log.warn("tool failed conv={} tool={} err={}", conversationId(), toolName, cause.getMessage());
            return observationFailed(cause.getMessage());
        } finally {
            logFinish(toolName, ok, (System.nanoTime() - started) / 1_000_000);
        }
    }

    public void logFinish(String toolName, boolean success, long elapsedMs) {
        log.info("tool conv={} name={} ok={} ms={}", conversationId(), toolName, success, elapsedMs);
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }

    static String observationFailed(String message) {
        String detail = message == null ? "unknown" : message.replace("\"", "'");
        if (detail.length() > 180) {
            detail = detail.substring(0, 180);
        }
        return "{\"error\":\"工具调用失败：" + detail + "，请根据已有信息回答\"}";
    }

    private static String conversationId() {
        State state = HOLDER.get();
        return state == null ? "-" : state.conversationId;
    }

    private static final class State {
        private String conversationId;
        private int count;
    }
}
