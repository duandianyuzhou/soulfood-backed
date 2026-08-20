package com.food.soulfoodbackend.config;

import com.food.soulfoodbackend.ai.CapacityFallbackChatModel;
import com.food.soulfoodbackend.ai.react.GuardedToolCallback;
import com.food.soulfoodbackend.ai.react.ToolCallGuard;
import com.food.soulfoodbackend.chat.PgChatMemoryRepository;
import com.food.soulfoodbackend.service.AiChatTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            你是 SoulFood 美食助手，擅长中餐推荐、菜谱讲解和饮食搭配建议。
            回答要简洁实用，语气亲切，优先给出可操作的推荐。
            结合对话历史理解用户意图。
            不要复述用户原话，不要把上一轮回答原文再输出一遍。
            """;

    @Bean
    public ChatMemory chatMemory(PgChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(30)
                .build();
    }

    @Bean
    public ToolCallback[] guardedAiTools(AiChatTools aiChatTools, ToolCallGuard toolCallGuard) {
        ToolCallback[] raw = ToolCallbacks.from(aiChatTools);
        ToolCallback[] guarded = new ToolCallback[raw.length];
        for (int i = 0; i < raw.length; i++) {
            guarded[i] = new GuardedToolCallback(raw[i], toolCallGuard);
        }
        return guarded;
    }

    @Bean
    public OllamaChatModel ollamaChatModel(
            ObjectProvider<OllamaApi> ollamaApi,
            @Value("${spring.ai.ollama.base-url:http://127.0.0.1:11434}") String ollamaBaseUrl,
            @Value("${spring.ai.ollama.chat.options.model:qwen3.5:2b}") String ollamaChatModel) {
        OllamaApi api = ollamaApi.getIfAvailable();
        if (api == null) {
            api = OllamaApi.builder().baseUrl(ollamaBaseUrl).build();
        }
        return OllamaChatModel.builder()
                .ollamaApi(api)
                .defaultOptions(OllamaChatOptions.builder()
                        .model(ollamaChatModel)
                        .temperature(0.7)
                        .build())
                .build();
    }

    @Bean
    @Primary
    public ChatModel chatModel(OpenAiChatModel openAiChatModel, OllamaChatModel ollamaChatModel) {
        return new CapacityFallbackChatModel(openAiChatModel, ollamaChatModel);
    }

    /** 复杂 ReAct（非流式）：智谱 GLM，429 时改 Ollama qwen；带工具 + 会话记忆 */
    @Bean
    public ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, ToolCallback[] guardedAiTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(guardedAiTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /** 流式 ReAct：智谱 GLM，429 时改 Ollama qwen；记忆由业务在流结束后写入 */
    @Bean
    public ChatClient toolStreamChatClient(ChatModel chatModel, ToolCallback[] guardedAiTools) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultToolCallbacks(guardedAiTools)
                .build();
    }

    /** 简单闲聊（非流式）：智谱 GLM，429 时改 Ollama qwen */
    @Bean
    public ChatClient simpleChatClient(ChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /** 一次性 / 流式生成：智谱 GLM，429 时改 Ollama qwen，无记忆 */
    @Bean
    public ChatClient statelessChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
