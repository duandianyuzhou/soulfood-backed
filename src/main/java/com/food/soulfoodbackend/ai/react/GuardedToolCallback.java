package com.food.soulfoodbackend.ai.react;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

public final class GuardedToolCallback implements ToolCallback {

    private static final String LIMIT_JSON = "{\"error\":\"工具调用次数已达上限，请根据已有结果直接回答用户\"}";

    private final ToolCallback delegate;
    private final ToolCallGuard guard;

    public GuardedToolCallback(ToolCallback delegate, ToolCallGuard guard) {
        this.delegate = delegate;
        this.guard = guard;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return invoke(() -> delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return invoke(() -> delegate.call(toolInput, toolContext));
    }

    private String invoke(java.util.function.Supplier<String> body) {
        String name = delegate.getToolDefinition().name();
        if (!guard.tryAcquire(name)) {
            return LIMIT_JSON;
        }
        long started = System.nanoTime();
        boolean ok = true;
        try {
            return body.get();
        } catch (RuntimeException ex) {
            ok = false;
            throw ex;
        } finally {
            guard.logFinish(name, ok, (System.nanoTime() - started) / 1_000_000);
        }
    }
}
