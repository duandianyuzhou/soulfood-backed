package com.food.soulfoodbackend.service;

import com.food.soulfoodbackend.mapper.SfAiChatMessageMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.tools.OrderTools;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.domain.entity.SfAiChatMessage;
import com.food.soulfoodbackend.dto.ChatResponse;
import com.food.soulfoodbackend.dto.ai.AiConversationItemDto;
import com.food.soulfoodbackend.dto.ai.ChatActionCardDto;
import com.food.soulfoodbackend.dto.ai.ChatHistoryMessageDto;
import com.food.soulfoodbackend.dto.ai.RandomPickRequest;
import com.food.soulfoodbackend.dto.ai.RandomPickResponse;
import com.food.soulfoodbackend.dto.ai.RecipeItemDto;
import com.food.soulfoodbackend.dto.ai.RecommendRecipesRequest;
import com.food.soulfoodbackend.dto.ai.RecommendRecipesResponse;
import com.food.soulfoodbackend.dto.ai.SuggestOptionsRequest;
import com.food.soulfoodbackend.dto.ai.SuggestOptionsResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.ArrayList;
import java.util.Objects;
import java.time.Duration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class AiChatService {

    private final ChatClient chatClient;
    private final ChatClient statelessChatClient;
    private final ChatMemory chatMemory;
    private final AiConversationService conversationService;
    private final AiChatContextService contextService;
    private final ChatActionCardResolver cardResolver;
    private final AiChatMessageMetaService messageMetaService;
    private final SfAiChatMessageMapper messageMapper;
    private final ObjectMapper objectMapper;
    private final AiMemoryExtractionService memoryExtractionService;
    private final Cache<String, RecommendRecipesResponse> recipeRecommendCache;
    private final OrderTools orderTools;

    @Value("${app.ai.vision-model:glm-4v-flash}")
    private String visionModel;

    public AiChatService(
            @Qualifier("chatClient") ChatClient chatClient,
            @Qualifier("statelessChatClient") ChatClient statelessChatClient,
            ChatMemory chatMemory,
            AiConversationService conversationService,
            AiChatContextService contextService,
            ChatActionCardResolver cardResolver,
            AiChatMessageMetaService messageMetaService,
            SfAiChatMessageMapper messageMapper,
            ObjectMapper objectMapper,
            AiMemoryExtractionService memoryExtractionService,
            OrderTools orderTools) {
        this.chatClient = chatClient;
        this.statelessChatClient = statelessChatClient;
        this.chatMemory = chatMemory;
        this.conversationService = conversationService;
        this.contextService = contextService;
        this.cardResolver = cardResolver;
        this.messageMetaService = messageMetaService;
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
        this.memoryExtractionService = memoryExtractionService;
        this.recipeRecommendCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(Duration.ofMinutes(3))
                .build();
        this.orderTools = orderTools;
    }

    public String resolveConversationId(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationId;
        }
        return UUID.randomUUID().toString();
    }

    public ChatResponse chat(
            String conversationId,
            String message,
            Long userId,
            Double lat,
            Double lng,
            String imageBase64,
            String imageMimeType) {
        String userText = normalizeMessage(message, imageBase64);
        prepareConversation(conversationId, userId, userText);
        boolean hasImage = hasImage(imageBase64);
        String systemPrompt = hasImage
                ? contextService.buildVisionSystemPrompt(userId, lat, lng)
                : contextService.buildSystemPrompt(userId, lat, lng);
        try {
            AiChatToolContextHolder.set(userId, lat, lng, conversationId);
            String reply = safeCall(() -> {
                var spec = chatClient.prompt().system(systemPrompt);
                applyUser(spec, userText, imageBase64, imageMimeType);
                if (hasImage) {
                    spec.options(visionOptions());
                }
                return spec.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                        .call()
                        .content();
            }, "今天可以试试番茄炒蛋，简单又下饭。");
            List<ChatActionCardDto> cards = cardResolver.resolve(reply, userId, lat, lng);
            messageMetaService.saveCardsOnLatestAssistant(conversationId, cards);
            memoryExtractionService.scheduleExtraction(userId, conversationId, userText, reply);
            return new ChatResponse(conversationId, reply, cards);
        } finally {
            AiChatToolContextHolder.clear();
        }
    }

    public Flux<String> chatStreamNdjson(
            String conversationId,
            String message,
            Long userId,
            Double lat,
            Double lng,
            String imageBase64,
            String imageMimeType) {
        String userText = normalizeMessage(message, imageBase64);
        prepareConversation(conversationId, userId, userText);
        boolean hasImage = hasImage(imageBase64);
        String systemPrompt = hasImage
                ? contextService.buildVisionSystemPrompt(userId, lat, lng)
                : contextService.buildSystemPrompt(userId, lat, lng);
        AtomicReference<StringBuilder> buffer = new AtomicReference<>(new StringBuilder());
        AiChatToolContextHolder.set(userId, lat, lng, conversationId);

        var requestSpec = chatClient.prompt().system(systemPrompt);
        applyUser(requestSpec, userText, imageBase64, imageMimeType);
        if (hasImage) {
            requestSpec.options(visionOptions());
        }

        Flux<String> chunks = requestSpec
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .stream()
                .content()
                .map(chunk -> {
                    buffer.get().append(chunk);
                    return toNdjson(Map.of("type", "chunk", "text", chunk));
                });

        Mono<String> done = Mono.fromCallable(() -> {
            try {
                String fullReply = buffer.get().toString();
                List<ChatActionCardDto> cards = cardResolver.resolve(fullReply, userId, lat, lng);
                messageMetaService.saveCardsOnLatestAssistant(conversationId, cards);
                memoryExtractionService.scheduleExtraction(userId, conversationId, userText, fullReply);
                Map<String, Object> payload = new HashMap<>();
                payload.put("type", "done");
                payload.put("conversationId", conversationId);
                payload.put("cards", cards);
                return toNdjson(payload);
            } finally {
                AiChatToolContextHolder.clear();
            }
        });

        return chunks.concatWith(done.flux())
                .doFinally(signal -> AiChatToolContextHolder.clear());
    }

    public Flux<String> chatStream(String conversationId, String message, Long userId) {
        return chatStreamNdjson(conversationId, message, userId, null, null, null, null)
                .filter(line -> line.contains("\"type\":\"chunk\""))
                .map(line -> extractChunkText(line));
    }

    private void applyUser(
            ChatClient.ChatClientRequestSpec spec,
            String message,
            String imageBase64,
            String imageMimeType) {
        if (!hasImage(imageBase64)) {
            spec.user(message);
            return;
        }
        byte[] bytes = decodeImage(imageBase64);
        MimeType mime = MimeType.valueOf(
                StringUtils.hasText(imageMimeType) ? imageMimeType : "image/jpeg");
        Media media = Media.builder().mimeType(mime).data(new ByteArrayResource(bytes)).build();
        spec.user(u -> u.text(message).media(media));
    }

    private ChatOptions visionOptions() {
        return OpenAiChatOptions.builder().model(visionModel).build();
    }

    private static boolean hasImage(String imageBase64) {
        return StringUtils.hasText(imageBase64);
    }

    private static String normalizeMessage(String message, String imageBase64) {
        if (StringUtils.hasText(message)) {
            return message.trim();
        }
        if (hasImage(imageBase64)) {
            return "请识别图片中的食材或菜品，并推荐可以做什么菜或怎么点单。";
        }
        return "";
    }

    private static byte[] decodeImage(String imageBase64) {
        String payload = imageBase64.trim();
        int comma = payload.indexOf(',');
        if (payload.startsWith("data:") && comma > 0) {
            payload = payload.substring(comma + 1);
        }
        byte[] bytes = Base64.getDecoder().decode(payload);
        if (bytes.length > 4 * 1024 * 1024) {
            throw new IllegalArgumentException("图片过大，请压缩后重试（最大 4MB）");
        }
        return bytes;
    }

    public List<ChatHistoryMessageDto> getHistoryMessages(
            String conversationId,
            Long userId,
            Integer offset,
            Integer limit) {
        conversationService.assertOwnedByUser(conversationId, userId);
        int safeOffset = offset == null ? 0 : Math.max(0, offset);
        int safeLimit = limit == null ? 10 : Math.max(1, Math.min(limit, 50));
        List<SfAiChatMessage> rows = messageMapper.selectList(new LambdaQueryWrapper<SfAiChatMessage>()
                .eq(SfAiChatMessage::getConversationId, conversationId)
                .in(SfAiChatMessage::getMessageType, "USER", "ASSISTANT")
                .orderByDesc(SfAiChatMessage::getSortOrder)
                .orderByDesc(SfAiChatMessage::getId)
                .last("LIMIT " + safeLimit + " OFFSET " + safeOffset));
        List<SfAiChatMessage> ascRows = new ArrayList<>(rows);
        java.util.Collections.reverse(ascRows);
        return ascRows.stream().map(this::toHistoryDto).toList();
    }

    private ChatHistoryMessageDto toHistoryDto(SfAiChatMessage row) {
        String role = "USER".equals(row.getMessageType()) ? "user" : "assistant";
        String content = row.getContent() != null ? row.getContent() : "";
        List<ChatActionCardDto> cards = messageMetaService.parseCards(row.getMetaJson());
        return new ChatHistoryMessageDto(role, content, cards);
    }

    public List<AiConversationItemDto> listConversations(Long userId, String keyword, String sceneTag) {
        return conversationService.listForUser(userId, keyword, sceneTag);
    }

    public void updateConversation(String conversationId, Long userId, String title, String sceneTag) {
        conversationService.updateConversation(conversationId, userId, title, sceneTag);
    }

    public void clearMemory(String conversationId, Long userId) {
        conversationService.assertOwnedByUser(conversationId, userId);
        chatMemory.clear(conversationId);
    }

    private void prepareConversation(String conversationId, Long userId, String message) {
        conversationService.ensureConversation(conversationId, userId);
        conversationService.setTitleIfBlank(conversationId, message);
    }

    private String toNdjson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            return "{\"type\":\"error\",\"message\":\"序列化失败\"}";
        }
    }

    private String extractChunkText(String line) {
        try {
            var node = objectMapper.readTree(line);
            return node.path("text").asText("");
        } catch (JsonProcessingException ex) {
            return "";
        }
    }

    public String recommend(String preference) {
        String prompt = """
                请根据以下饮食偏好推荐 3 道适合的家常菜，并简要说明理由：
                %s
                """.formatted(preference);
        return safeCall(() -> statelessChatClient.prompt()
                .user(prompt)
                .call()
                .content(), fallbackRecipeText(preference, null));
    }

    public RecommendRecipesResponse recommendRecipes(RecommendRecipesRequest request) {
        String preference = request.getPreference() != null ? request.getPreference() : "清淡、家常菜";
        String winner = request.getVoteWinner() != null ? request.getVoteWinner() : "";
        String cacheKey = preference + "|" + winner + "|" + Objects.toString(request.getParticipants(), "");
        RecommendRecipesResponse cached = recipeRecommendCache.getIfPresent(cacheKey);
        if (cached != null) {
            return cached;
        }
        String prompt = """
                根据饮食偏好「%s」%s，推荐 3 道可在家做的家常菜。
                严格只输出 JSON，不要其他文字：
                {"items":[{"name":"菜名","score":4.8,"reason":"一句理由"}]}
                """.formatted(
                preference,
                winner.isBlank() ? "" : "，投票胜出选项是「" + winner + "」");

        String raw;
        boolean aiFailed;
        try {
            raw = statelessChatClient.prompt().user(prompt).call().content();
            aiFailed = raw == null || raw.isBlank();
        } catch (Exception ex) {
            log.warn("AI recommendRecipes failed: {}", ex.getMessage());
            aiFailed = true;
            raw = "";
        }

        List<RecipeItemDto> items;
        if (aiFailed) {
            items = defaultRecipeItems(winner);
            raw = formatRecipeReply(items);
        } else {
            items = AiResponseParser.parseRecipes(raw);
            if (items.isEmpty()) {
                items = defaultRecipeItems(winner);
                raw = formatRecipeReply(items);
            }
        }
        RecommendRecipesResponse response = new RecommendRecipesResponse(request.getConversationId(), items, raw);
        recipeRecommendCache.put(cacheKey, response);
        return response;
    }

    public SuggestOptionsResponse suggestOptions(SuggestOptionsRequest request) {
        String topic = request.getTopic() != null ? request.getTopic() : "今晚吃什么";
        String existing = request.getExistingOptions() != null
                ? String.join("、", request.getExistingOptions())
                : "无";
        String prompt = """
                为聚餐主题「%s」再生成 3 个投票选项（每个 2-8 个字）。
                已有选项：%s
                每行只写一个选项名称，不要编号和解释。
                """.formatted(topic, existing);
        String raw = safeCall(() -> statelessChatClient.prompt().user(prompt)
                .tools(orderTools)
                .call().content(), "寿司\n麻辣烫\n黄焖鸡");
        List<String> options = AiResponseParser.parseOptions(raw);
        if (options.isEmpty()) {
            options = List.of("寿司", "麻辣烫", "黄焖鸡");
        }
        return new SuggestOptionsResponse(options, raw);
    }

    public RandomPickResponse randomPick(RandomPickRequest request) {
        List<String> candidates = request.getCandidates() != null ? request.getCandidates() : List.of();
        if (candidates.isEmpty()) {
            return new RandomPickResponse("暂无推荐", "候选列表为空", "");
        }
        String pref = request.getPreference() != null ? request.getPreference() : "";
        String prompt = """
                从以下餐厅中选一家今天去吃，结合偏好「%s」。
                候选：%s
                严格只输出 JSON：{"name":"店名","reason":"一句话理由"}
                """.formatted(pref, String.join("、", candidates));
        String raw = safeCall(() -> statelessChatClient.prompt().user(prompt).call().content(),
                "{\"name\":\"" + candidates.get(0) + "\",\"reason\":\"距离近、评分不错\"}");
        AiResponseParser.RandomPickResult pick = AiResponseParser.parsePick(raw, candidates);
        return new RandomPickResponse(pick.name(), pick.reason(), pick.rawReply());
    }

    private String safeCall(java.util.function.Supplier<String> supplier, String fallback) {
        try {
            String result = supplier.get();
            if (result == null || result.isBlank()) {
                return fallback;
            }
            return result;
        } catch (Exception ex) {
            log.warn("AI call failed, using fallback: {}", ex.getMessage());
            return fallback;
        }
    }

    private List<RecipeItemDto> defaultRecipeItems(String winner) {
        if (winner != null && winner.contains("火锅")) {
            return List.of(
                    new RecipeItemDto("毛肚", 4.8, "火锅经典，口感爽脆"),
                    new RecipeItemDto("肥牛卷", 4.7, "搭配火锅很合适"),
                    new RecipeItemDto("娃娃菜", 4.6, "解腻必备"));
        }
        return List.of(
                new RecipeItemDto("番茄炒蛋", 4.8, "清淡下饭"),
                new RecipeItemDto("土豆丝炒肉", 4.6, "家常菜"),
                new RecipeItemDto("青菜鸡蛋面", 4.7, "快手暖胃"));
    }

    private String formatRecipeReply(List<RecipeItemDto> items) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            RecipeItemDto item = items.get(i);
            sb.append(i + 1).append(". ").append(item.getName()).append("：").append(item.getReason());
            if (i < items.size() - 1) {
                sb.append('\n');
            }
        }
        return sb.toString();
    }

    private String fallbackRecipeText(String preference, String winner) {
        return String.join("\n",
                "1. 番茄炒蛋：清淡下饭",
                "2. 土豆丝炒肉：家常菜",
                "3. 青菜鸡蛋面：快手暖胃")
                + (winner != null && !winner.isBlank() ? "（搭配「" + winner + "」）" : "");
    }
}
