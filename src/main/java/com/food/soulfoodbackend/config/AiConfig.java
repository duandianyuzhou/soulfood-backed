package com.food.soulfoodbackend.config;

import com.food.soulfoodbackend.chat.PgChatMemoryRepository;
import com.food.soulfoodbackend.service.AiChatTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    private static final String SYSTEM_PROMPT = """
            你是 SoulFood 美食助手，擅长中餐推荐、菜谱讲解和饮食搭配建议。
            回答要简洁实用，语气亲切，优先给出可操作的推荐。
            不要复述用户原话，不要把同一段建议写两遍。
            """;

    @Bean
    public ChatMemory chatMemory(PgChatMemoryRepository chatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(30)
                .build();
    }

    /** 复杂 ReAct（非流式）：智谱 GLM + 工具 + 会话记忆 */
    @Bean
    public ChatClient chatClient(OpenAiChatModel openAiChatModel, ChatMemory chatMemory, AiChatTools aiChatTools) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(aiChatTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /** 流式 ReAct：智谱 GLM + 工具；记忆由业务在流结束后写入，避免 advisor 把同一轮存两遍 */
    @Bean
    public ChatClient toolStreamChatClient(OpenAiChatModel openAiChatModel, AiChatTools aiChatTools) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultTools(aiChatTools)
                .build();
    }

    /** 简单闲聊（非流式）：智谱 GLM + 会话记忆，不带工具 */
    @Bean
    public ChatClient simpleChatClient(OpenAiChatModel openAiChatModel, ChatMemory chatMemory) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    /** 一次性 / 流式生成：智谱 GLM，无记忆 */
    @Bean
    public ChatClient statelessChatClient(OpenAiChatModel openAiChatModel) {
        return ChatClient.builder(openAiChatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
