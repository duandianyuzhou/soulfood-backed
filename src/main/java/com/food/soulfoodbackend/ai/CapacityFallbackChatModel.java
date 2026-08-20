package com.food.soulfoodbackend.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 智谱等主模型 HTTP 429 / 1305 访问量过大时，改走本地 Ollama。
 */
@Slf4j
public class CapacityFallbackChatModel implements ChatModel {

    private final ChatModel primary;
    private final ChatModel fallback;

    public CapacityFallbackChatModel(ChatModel primary, ChatModel fallback) {
        this.primary = primary;
        this.fallback = fallback;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        try {
            return primary.call(prompt);
        } catch (RuntimeException ex) {
            if (!canFallback(ex)) {
                throw ex;
            }
            log.warn("智谱访问量过大，改用本地 Ollama {}", fallbackModel());
            try {
                return fallback.call(fallbackPrompt(prompt));
            } catch (RuntimeException fallbackEx) {
                log.warn("本地 Ollama 回退失败: {}", fallbackEx.getMessage());
                throw ex;
            }
        }
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        AtomicBoolean emitted = new AtomicBoolean(false);
        return primary.stream(prompt)
                .doOnNext(ignored -> emitted.set(true))
                .onErrorResume(ex -> {
                    if (emitted.get() || !canFallback(ex)) {
                        return Flux.error(ex);
                    }
                    log.warn("智谱流式访问量过大，改用本地 Ollama {}", fallbackModel());
                    return fallback.stream(fallbackPrompt(prompt))
                            .onErrorResume(fallbackEx -> {
                                log.warn("本地 Ollama 流式回退失败: {}", fallbackEx.getMessage());
                                return Flux.error(ex);
                            });
                });
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return primary.getDefaultOptions();
    }

    private boolean canFallback(Throwable error) {
        return fallback != null && CapacityExceeded.of(error);
    }

    private Prompt fallbackPrompt(Prompt prompt) {
        List<Message> instructions = prompt.getInstructions();
        ChatOptions options = fallback.getDefaultOptions();
        if (options == null) {
            return new Prompt(instructions);
        }
        return new Prompt(instructions, options);
    }

    private String fallbackModel() {
        ChatOptions options = fallback.getDefaultOptions();
        if (options == null || options.getModel() == null) {
            return "qwen";
        }
        return options.getModel();
    }
}
