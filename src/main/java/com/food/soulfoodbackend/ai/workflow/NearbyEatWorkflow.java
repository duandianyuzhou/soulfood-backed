package com.food.soulfoodbackend.ai.workflow;

import com.food.soulfoodbackend.ai.stream.ChatStreamEmitter;
import com.food.soulfoodbackend.dto.ai.ChatActionCardDto;
import com.food.soulfoodbackend.dto.ai.WorkflowSnapshotDto;
import com.food.soulfoodbackend.dto.preference.PreferenceResponse;
import com.food.soulfoodbackend.dto.restaurant.RestaurantDto;
import com.food.soulfoodbackend.service.RestaurantService;
import com.food.soulfoodbackend.service.UserPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class NearbyEatWorkflow {

    private static final int MAX_CARDS = 5;

    private final RestaurantService restaurantService;
    private final UserPreferenceService preferenceService;
    private final ChatStreamEmitter streamEmitter;

    public WorkflowResult run(String conversationId, String userText, Long userId, Double lat, Double lng) {
        String runId = WorkflowSupport.runId("nearby");
        WorkflowSnapshotDto snapshot = new WorkflowSnapshotDto(runId, "今晚吃什么", "running", new ArrayList<>());
        List<String> events = new ArrayList<>();
        events.add(streamEmitter.workflow(runId, snapshot.getTitle(), "running"));

        WorkflowSupport.upsertStep(streamEmitter, events, snapshot, "locate", "确认位置", "running", null);
        if (lat == null || lng == null) {
            WorkflowSupport.completeStep(streamEmitter, events, snapshot, "locate", "failed", "未授权定位");
            String reply = "要推荐附近餐厅，需要先开启定位。你也可以直接说想吃的品类，或去「附近觅食」页授权位置。";
            snapshot.setStatus("failed");
            events.add(streamEmitter.workflow(runId, snapshot.getTitle(), "failed"));
            events.add(streamEmitter.chunk(reply));
            return new WorkflowResult(events, reply, List.of(), snapshot, false);
        }
        WorkflowSupport.completeStep(streamEmitter, events, snapshot, "locate", "done",
                String.format(Locale.ROOT, "已定位 %.4f, %.4f", lat, lng));

        WorkflowSupport.upsertStep(streamEmitter, events, snapshot, "nearby_search", "搜附近餐厅", "running", null);
        List<RestaurantDto> found;
        String keyword = extractKeyword(userText);
        try {
            found = restaurantService.listNearby(userId, lng, lat, null, keyword);
        } catch (Exception ex) {
            log.warn("nearby_search failed: {}", ex.getMessage());
            WorkflowSupport.completeStep(streamEmitter, events, snapshot, "nearby_search", "failed", "搜索失败");
            String reply = "附近餐厅暂时搜不到，请稍后再试，或换个关键词。";
            snapshot.setStatus("failed");
            events.add(streamEmitter.workflow(runId, snapshot.getTitle(), "failed"));
            events.add(streamEmitter.chunk(reply));
            return new WorkflowResult(events, reply, List.of(), snapshot, false);
        }
        WorkflowSupport.completeStep(streamEmitter, events, snapshot, "nearby_search", "done",
                found.isEmpty() ? "没有找到店" : "找到 " + found.size() + " 家");

        WorkflowSupport.upsertStep(streamEmitter, events, snapshot, "preference_filter", "按口味过滤", "running", null);
        PreferenceResponse pref = userId == null ? null : preferenceService.getPreference(userId);
        List<RestaurantDto> filtered = filterByPreference(found, pref);
        if (filtered.isEmpty()) {
            filtered = found;
        }
        String prefSummary = pref == null ? "未登录，按大众口味" : pref.getPreferenceText();
        WorkflowSupport.completeStep(streamEmitter, events, snapshot, "preference_filter", "done", prefSummary);

        WorkflowSupport.upsertStep(streamEmitter, events, snapshot, "present_options", "给出推荐", "running", null);
        List<RestaurantDto> top = filtered.stream().limit(MAX_CARDS).toList();
        List<ChatActionCardDto> cards = new ArrayList<>();
        StringBuilder reply = new StringBuilder();
        if (top.isEmpty()) {
            reply.append("附近暂时没有合适的店。换个品类，或到「附近觅食」再试一次。");
        } else {
            reply.append("结合你的口味，这几家比较合适：\n");
            for (int i = 0; i < top.size(); i++) {
                RestaurantDto item = top.get(i);
                String distance = item.getDistanceKm() == null ? "" : item.getDistanceKm() + "km";
                reply.append(i + 1).append(". ").append(item.getName());
                if (StringUtils.hasText(item.getCategory())) {
                    reply.append("（").append(item.getCategory()).append("）");
                }
                if (!distance.isEmpty()) {
                    reply.append(" · ").append(distance);
                }
                reply.append('\n');
                cards.add(new ChatActionCardDto(
                        "restaurant",
                        item.getId(),
                        item.getName(),
                        distance.isEmpty() ? item.getCategory() : distance));
            }
        }
        WorkflowSupport.completeStep(streamEmitter, events, snapshot, "present_options", "done",
                top.isEmpty() ? "无推荐" : "推荐 " + top.size() + " 家");
        snapshot.setStatus("done");
        events.add(streamEmitter.workflow(runId, snapshot.getTitle(), "done"));
        if (!cards.isEmpty()) {
            events.add(streamEmitter.cards(cards));
        }
        events.add(streamEmitter.chunk(reply.toString().trim()));
        return new WorkflowResult(events, reply.toString().trim(), cards, snapshot, false);
    }

    static String extractKeyword(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String[] keys = {"火锅", "烧烤", "烤肉", "日料", "寿司", "面", "米线", "川菜", "粤菜", "西餐", "咖啡", "甜品", "海鲜"};
        for (String key : keys) {
            if (text.contains(key)) {
                return key;
            }
        }
        return null;
    }

    static List<RestaurantDto> filterByPreference(List<RestaurantDto> items, PreferenceResponse pref) {
        if (items == null || items.isEmpty() || pref == null) {
            return items == null ? List.of() : items;
        }
        List<RestaurantDto> kept = new ArrayList<>();
        for (RestaurantDto item : items) {
            String blob = ((item.getName() == null ? "" : item.getName())
                    + " "
                    + (item.getCategory() == null ? "" : item.getCategory())).toLowerCase(Locale.ROOT);
            if (pref.isNoCoriander() && blob.contains("香菜")) {
                continue;
            }
            if (pref.isNoPeanut() && (blob.contains("花生") || blob.contains("坚果"))) {
                continue;
            }
            if (pref.getSpicyLevel() <= 2 && (blob.contains("麻辣") || blob.contains("变态辣"))) {
                continue;
            }
            boolean allergenHit = pref.getAllergens() != null && pref.getAllergens().stream()
                    .filter(StringUtils::hasText)
                    .anyMatch(a -> blob.contains(a.toLowerCase(Locale.ROOT)));
            if (allergenHit) {
                continue;
            }
            kept.add(item);
        }
        return kept;
    }
}
