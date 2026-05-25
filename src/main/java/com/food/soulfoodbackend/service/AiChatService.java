package com.food.soulfoodbackend.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public AiChatService(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    public String resolveConversationId(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationId;
        }
        return UUID.randomUUID().toString();
    }

    public String chat(String conversationId, String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
    }

    public Flux<String> chatStream(String conversationId, String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content();
    }

    public List<Message> getHistory(String conversationId) {
        return chatMemory.get(conversationId);
    }

    public void clearMemory(String conversationId) {
        chatMemory.clear(conversationId);
    }

    public String recommend(String preference) {
        String prompt = """
                请根据以下饮食偏好推荐 3 道适合的家常菜，并简要说明理由：
                %s
                """.formatted(preference);
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
