package com.food.soulfoodbackend.ai.react;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

public final class GuardedToolCallback implements ToolCallback {

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
        return guard.execute(delegate.getToolDefinition().name(), () -> delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        return guard.execute(delegate.getToolDefinition().name(), () -> delegate.call(toolInput, toolContext));
    }
}
