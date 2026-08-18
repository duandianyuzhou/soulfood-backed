package com.food.soulfoodbackend.config;

import com.food.soulfoodbackend.chat.PgChatMemoryRepository;
import com.food.soulfoodbackend.service.AiChatTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            你是 SoulFood 美食助手，擅长中餐推荐、菜谱讲解和饮食搭配建议。
            回答要简洁实用，语气亲切，优先给出可操作的推荐。
            """;

    @Bean
    public ChatMemory chatMemory(PgChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(30)
                .build();
    }

    /** 复杂 ReAct：智谱 GLM + 工具 */
    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel, ChatMemory chatMemory, AiChatTools aiChatTools) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(aiChatTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /** 简单闲聊：本地 Ollama + 会话记忆，不带工具 */
    @Bean
    public ChatClient simpleChatClient(OllamaChatModel ollamaChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(ollamaChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /** 一次性调用：本地 Ollama，无记忆 */
    @Bean
    public ChatClient statelessChatClient(OllamaChatModel ollamaChatModel) {
        return ChatClient.builder(ollamaChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
