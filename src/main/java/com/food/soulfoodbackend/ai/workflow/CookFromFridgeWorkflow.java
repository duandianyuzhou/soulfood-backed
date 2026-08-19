package com.food.soulfoodbackend.ai.workflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.ai.rag.RagHit;
import com.food.soulfoodbackend.ai.rag.RagSearchService;
import com.food.soulfoodbackend.ai.router.ChatIntent;
import com.food.soulfoodbackend.ai.stream.ChatStreamEmitter;
import com.food.soulfoodbackend.dto.ai.ChatActionCardDto;
import com.food.soulfoodbackend.dto.ai.WorkflowSnapshotDto;
import com.food.soulfoodbackend.dto.recipe.RecipeSummaryDto;
import com.food.soulfoodbackend.service.RecipeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class CookFromFridgeWorkflow {

    private static final int MAX_CARDS = 5;

    private final ChatClient statelessChatClient;
    private final RagSearchService ragSearchService;
    private final RecipeService recipeService;
    private final ChatStreamEmitter streamEmitter;
    private final PendingWorkflowStore pendingStore;

    @Value("${app.ai.vision-model:glm-4v-flash}")
    private String visionModel;

    public CookFromFridgeWorkflow(
            @Qualifier("statelessChatClient") ChatClient statelessChatClient,
            RagSearchService ragSearchService,
            RecipeService recipeService,
            ChatStreamEmitter streamEmitter,
            PendingWorkflowStore pendingStore) {
        this.statelessChatClient = statelessChatClient;
        this.ragSearchService = ragSearchService;
        this.recipeService = recipeService;
        this.streamEmitter = streamEmitter;
        this.pendingStore = pendingStore;
    }

    public WorkflowResult start(
            String conversationId,
            String userText,
            Long userId,
            String imageBase64,
            String imageMimeType) {
        String runId = WorkflowSupport.runId("fridge");
        WorkflowSnapshotDto snapshot = new WorkflowSnapshotDto(runId, "冰箱做菜", "running", new ArrayList<>());
        List<String> events = new ArrayList<>();
        events.add(streamEmitter.workflow(runId, snapshot.getTitle(), "running"));
        return runFromVision(events, snapshot, conversationId, userText, userId, imageBase64, imageMimeType);
    }

    public WorkflowResult resume(PendingWorkflowSession session, String imageBase64, String imageMimeType) {
        WorkflowSnapshotDto snapshot = session.getSnapshot();
        List<String> events = new ArrayList<>();
        events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "running"));
        snapshot.setStatus("running");
        return runFromVision(
                events,
                snapshot,
                session.getConversationId(),
                session.getUserText(),
                session.getUserId(),
                imageBase64,
                imageMimeType);
    }

    private WorkflowResult runFromVision(
            List<String> events,
            WorkflowSnapshotDto snapshot,
            String conversationId,
            String userText,
            Long userId,
            String imageBase64,
            String imageMimeType) {
        WorkflowSupport.upsertStep(streamEmitter, events, snapshot, "vision", "识别食材", "running", null);
        if (!StringUtils.hasText(imageBase64)) {
            WorkflowSupport.completeStep(streamEmitter, events, snapshot, "vision", "waiting", "请发一张冰箱或菜单照片");
            snapshot.setStatus("waiting");
            events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "waiting"));
            String reply = "发一张冰箱、备餐台或菜单的照片，我按里面的食材给你配菜。";
            events.add(streamEmitter.chunk(reply));
            savePending(snapshot, conversationId, userText, userId);
            return new WorkflowResult(events, reply, List.of(), snapshot, true);
        }

        List<String> ingredients;
        try {
            ingredients = recognizeIngredients(userText, imageBase64, imageMimeType);
        } catch (Exception ex) {
            log.warn("fridge vision failed: {}", ex.getMessage());
            WorkflowSupport.completeStep(streamEmitter, events, snapshot, "vision", "failed", "识图失败");
            snapshot.setStatus("failed");
            events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "failed"));
            String reply = "这张图没看清食材。换个光线好一点的角度再试一次。";
            events.add(streamEmitter.chunk(reply));
            pendingStore.remove(snapshot.getRunId());
            return new WorkflowResult(events, reply, List.of(), snapshot, false);
        }
        if (ingredients.isEmpty()) {
            WorkflowSupport.completeStep(streamEmitter, events, snapshot, "vision", "failed", "未识别到食材");
            snapshot.setStatus("failed");
            events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "failed"));
            String reply = "没认出能做菜的食材。拍近一点，或在文字里补上食材名。";
            events.add(streamEmitter.chunk(reply));
            pendingStore.remove(snapshot.getRunId());
            return new WorkflowResult(events, reply, List.of(), snapshot, false);
        }
        WorkflowSupport.completeStep(
                streamEmitter, events, snapshot, "vision", "done", String.join("、", ingredients));

        WorkflowSupport.upsertStep(streamEmitter, events, snapshot, "rag_recipes", "检索菜谱", "running", null);
        String query = String.join(" ", ingredients);
        List<RagHit> hits = safeSearch(query);
        List<RecipeSummaryDto> recipes = recipeService.list(null, ingredients.get(0));
        WorkflowSupport.completeStep(
                streamEmitter,
                events,
                snapshot,
                "rag_recipes",
                "done",
                hits.isEmpty() && recipes.isEmpty() ? "库里匹配较少" : "已匹配菜谱");

        WorkflowSupport.upsertStep(streamEmitter, events, snapshot, "present_options", "给出做法", "running", null);
        List<ChatActionCardDto> cards = toCards(hits, recipes);
        StringBuilder reply = new StringBuilder();
        reply.append("从图里看到：").append(String.join("、", ingredients)).append("。\n");
        if (cards.isEmpty()) {
            reply.append("知识库暂时没有特别合适的成品菜，你可以按这些食材做个快炒，或换张更清晰的图。");
        } else {
            reply.append("比较适合现在做的：\n");
            for (int i = 0; i < cards.size(); i++) {
                ChatActionCardDto card = cards.get(i);
                reply.append(i + 1).append(". ").append(card.getName());
                if (StringUtils.hasText(card.getSubtitle())) {
                    reply.append("（").append(card.getSubtitle()).append("）");
                }
                reply.append('\n');
            }
        }
        WorkflowSupport.completeStep(
                streamEmitter,
                events,
                snapshot,
                "present_options",
                "done",
                cards.isEmpty() ? "无推荐" : "推荐 " + cards.size() + " 道");
        snapshot.setStatus("done");
        events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "done"));
        if (!cards.isEmpty()) {
            events.add(streamEmitter.cards(cards));
        }
        events.add(streamEmitter.chunk(reply.toString().trim()));
        pendingStore.remove(snapshot.getRunId());
        return new WorkflowResult(events, reply.toString().trim(), cards, snapshot, false);
    }

    private void savePending(
            WorkflowSnapshotDto snapshot, String conversationId, String userText, Long userId) {
        PendingWorkflowSession session = new PendingWorkflowSession();
        session.setRunId(snapshot.getRunId());
        session.setKind(ChatIntent.COOK_FROM_FRIDGE);
        session.setConversationId(conversationId);
        session.setUserId(userId);
        session.setWaitingStepId("vision");
        session.setUserText(userText);
        session.setSnapshot(snapshot);
        pendingStore.put(session);
    }

    private List<RagHit> safeSearch(String query) {
        try {
            return ragSearchService.search(query);
        } catch (Exception ex) {
            log.warn("fridge rag failed: {}", ex.getMessage());
            return List.of();
        }
    }

    private List<String> recognizeIngredients(String userText, String imageBase64, String imageMimeType) {
        byte[] bytes = decodeImage(imageBase64);
        MimeType mime = MimeType.valueOf(StringUtils.hasText(imageMimeType) ? imageMimeType : "image/jpeg");
        Media media = Media.builder().mimeType(mime).data(new ByteArrayResource(bytes)).build();
        String prompt = """
                识别图中可烹饪的食材或菜单菜名。只输出 JSON：
                {"ingredients":["番茄","鸡蛋"]}
                用户补充：%s
                """.formatted(StringUtils.hasText(userText) ? userText : "无");
        String raw = statelessChatClient.prompt()
                .options(OpenAiChatOptions.builder().model(visionModel).temperature(0.1).build())
                .user(u -> u.text(prompt).media(media))
                .call()
                .content();
        return parseIngredients(raw);
    }

    static List<String> parseIngredients(String raw) {
        if (!StringUtils.hasText(raw)) {
            return List.of();
        }
        try {
            String json = raw.trim();
            int start = json.indexOf('{');
            int end = json.lastIndexOf('}');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            JsonNode node = new ObjectMapper().readTree(json);
            JsonNode arr = node.path("ingredients");
            List<String> items = new ArrayList<>();
            if (arr.isArray()) {
                for (JsonNode item : arr) {
                    String name = item.asText("").trim();
                    if (name.length() >= 1 && name.length() <= 12 && !items.contains(name)) {
                        items.add(name);
                    }
                }
            }
            return items;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private static List<ChatActionCardDto> toCards(List<RagHit> hits, List<RecipeSummaryDto> recipes) {
        Map<Long, ChatActionCardDto> unique = new LinkedHashMap<>();
        if (hits != null) {
            for (RagHit hit : hits) {
                if ("recipe".equals(hit.getSourceType()) && hit.getSourceId() != null) {
                    unique.putIfAbsent(
                            hit.getSourceId(),
                            new ChatActionCardDto("recipe", hit.getSourceId(), hit.getTitle(), "知识库"));
                }
            }
        }
        if (recipes != null) {
            for (RecipeSummaryDto recipe : recipes) {
                unique.putIfAbsent(
                        recipe.getId(),
                        new ChatActionCardDto("recipe", recipe.getId(), recipe.getName(), recipe.getCategory()));
            }
        }
        return unique.values().stream().limit(MAX_CARDS).toList();
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
}
