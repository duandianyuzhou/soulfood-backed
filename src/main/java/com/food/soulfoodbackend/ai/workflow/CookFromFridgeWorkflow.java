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
import java.util.Set;

@Component
@Slf4j
public class CookFromFridgeWorkflow {

    private static final int MAX_CARDS = 5;
    private static final Set<String> SKIP_PHOTO = Set.of(
            "不拍图", "不拍照", "不发图", "不照相", "没图", "没有图", "没有照片",
            "懒得拍", "不拍了", "跳过", "用文字", "文字就行", "按食材", "按食材做", "口述食材");
    private static final List<String> KNOWN_INGREDIENTS = List.of(
            "番茄", "西红柿", "鸡蛋", "土豆", "土豆丝", "青椒", "辣椒", "豆腐", "牛肉", "猪肉",
            "鸡肉", "排骨", "茄子", "白菜", "生菜", "黄瓜", "洋葱", "蒜苔", "豆角", "蘑菇",
            "香菇", "虾", "鱼", "米饭", "面条");

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
        WorkflowSnapshotDto snapshot = new WorkflowSnapshotDto(
                runId, titleOf(userText, StringUtils.hasText(imageBase64)), "running", new ArrayList<>());
        List<String> events = new ArrayList<>();
        events.add(streamEmitter.workflow(runId, snapshot.getTitle(), "running"));
        return runFromVision(events, snapshot, conversationId, userText, userId, imageBase64, imageMimeType);
    }

    public WorkflowResult resume(
            PendingWorkflowSession session,
            String imageBase64,
            String imageMimeType,
            String extraMessage) {
        WorkflowSnapshotDto snapshot = session.getSnapshot();
        List<String> events = new ArrayList<>();
        events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "running"));
        snapshot.setStatus("running");
        String merged = mergeText(session.getUserText(), extraMessage);
        return runFromVision(
                events,
                snapshot,
                session.getConversationId(),
                merged,
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
        boolean hasImage = StringUtils.hasText(imageBase64);
        List<String> textIngredients = extractTextIngredients(userText);
        boolean skipPhoto = isSkipPhoto(userText);

        if (!hasImage && !skipPhoto && textIngredients.isEmpty()) {
            WorkflowSupport.completeStep(streamEmitter, events, snapshot, "vision", "waiting", "可拍照，或直接说不拍图");
            snapshot.setStatus("waiting");
            events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "waiting"));
            String reply = "可以发冰箱、菜单或菜品照片；也可以说「不拍图」「看图做菜」「按食材做菜」，或直接打出食材，例如「番茄鸡蛋」。";
            events.add(streamEmitter.chunk(reply));
            savePending(snapshot, conversationId, userText, userId);
            return new WorkflowResult(events, reply, List.of(), snapshot, true);
        }

        List<String> ingredients = new ArrayList<>(textIngredients);
        String visionSummary;
        if (hasImage) {
            try {
                List<String> fromImage = recognizeIngredients(userText, imageBase64, imageMimeType);
                for (String item : fromImage) {
                    if (!ingredients.contains(item)) {
                        ingredients.add(item);
                    }
                }
            } catch (Exception ex) {
                log.warn("fridge vision failed: {}", ex.getMessage());
                if (ingredients.isEmpty()) {
                    WorkflowSupport.completeStep(streamEmitter, events, snapshot, "vision", "failed", "识图失败");
                    snapshot.setStatus("failed");
                    events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "failed"));
                    String reply = "这张图没看清食材。换个角度，或直接说「不拍图」改用文字。";
                    events.add(streamEmitter.chunk(reply));
                    pendingStore.remove(snapshot.getRunId());
                    return new WorkflowResult(events, reply, List.of(), snapshot, false);
                }
            }
            visionSummary = ingredients.isEmpty() ? "已拍照但未识别到食材" : String.join("、", ingredients);
        } else {
            visionSummary = skipPhoto && ingredients.isEmpty() ? "未拍照，按家常菜推荐" : "未拍照，" + String.join("、", ingredients);
        }

        if (ingredients.isEmpty() && !hasImage) {
            ingredients = List.of("家常菜");
        }
        if (ingredients.isEmpty()) {
            WorkflowSupport.completeStep(streamEmitter, events, snapshot, "vision", "failed", "未识别到食材");
            snapshot.setStatus("failed");
            events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "failed"));
            String reply = "没认出能做菜的食材。拍近一点，或直接打出食材名。";
            events.add(streamEmitter.chunk(reply));
            pendingStore.remove(snapshot.getRunId());
            return new WorkflowResult(events, reply, List.of(), snapshot, false);
        }
        WorkflowSupport.completeStep(streamEmitter, events, snapshot, "vision", "done", visionSummary);

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
        boolean usedPhoto = StringUtils.hasText(imageBase64);
        StringBuilder reply = new StringBuilder();
        if (usedPhoto) {
            reply.append("从图里看到：").append(displayIngredients(ingredients)).append("。\n");
        } else if (ingredients.size() == 1 && "家常菜".equals(ingredients.get(0))) {
            reply.append("没拍照也没关系，先按家常菜给你几道：\n");
        } else {
            reply.append("按你说的食材：").append(displayIngredients(ingredients)).append("。\n");
        }
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

    static String titleOf(String text, boolean hasImage) {
        String raw = text == null ? "" : text;
        if (raw.contains("冰箱")) {
            return "冰箱做菜";
        }
        if (hasImage || containsAny(raw, "看图", "照片", "菜单", "识图", "识菜", "拍照")) {
            return "看图做菜";
        }
        return "按食材做菜";
    }

    private static boolean containsAny(String text, String... keys) {
        for (String key : keys) {
            if (text.contains(key)) {
                return true;
            }
        }
        return false;
    }

    static boolean isSkipPhoto(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String trimmed = text.trim();
        for (String key : SKIP_PHOTO) {
            if (trimmed.contains(key)) {
                return true;
            }
        }
        return false;
    }

    static List<String> extractTextIngredients(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        List<String> found = new ArrayList<>();
        for (String name : KNOWN_INGREDIENTS) {
            if (text.contains(name) && !found.contains(name)) {
                found.add(name);
            }
        }
        return found;
    }

    private static String displayIngredients(List<String> ingredients) {
        return ingredients.stream().filter(item -> !"家常菜".equals(item)).reduce((a, b) -> a + "、" + b)
                .orElse(String.join("、", ingredients));
    }

    private static String mergeText(String original, String extra) {
        if (!StringUtils.hasText(extra)) {
            return original;
        }
        if (!StringUtils.hasText(original)) {
            return extra;
        }
        return original + " " + extra;
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
