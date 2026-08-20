package com.food.soulfoodbackend.ai.workflow;

import com.food.soulfoodbackend.ai.router.ChatIntent;
import com.food.soulfoodbackend.ai.stream.ChatStreamEmitter;
import com.food.soulfoodbackend.dto.ai.ChatActionCardDto;
import com.food.soulfoodbackend.dto.ai.WorkflowSnapshotDto;
import com.food.soulfoodbackend.dto.restaurant.RestaurantDto;
import com.food.soulfoodbackend.dto.room.CreateRoomRequest;
import com.food.soulfoodbackend.dto.room.CreateRoomResponse;
import com.food.soulfoodbackend.service.RestaurantService;
import com.food.soulfoodbackend.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoteRoomWorkflow {

    private static final List<String> FALLBACK_OPTIONS = List.of("火锅", "烧烤", "日料");
    private static final Pattern SPLIT = Pattern.compile("、|,|，|/|\\||还是|或者|或是|和");
    private static final Set<String> STOP = Set.of(
            "投票", "组局", "开房", "开个房", "创建房间", "建个房", "一起吃", "帮我", "今晚吃什么", "吃什么");

    private final RoomService roomService;
    private final RestaurantService restaurantService;
    private final ChatStreamEmitter streamEmitter;
    private final PendingWorkflowStore pendingStore;

    public WorkflowResult start(
            String conversationId,
            String userText,
            Long userId,
            Double lat,
            Double lng) {
        String runId = WorkflowSupport.runId("vote");
        WorkflowSnapshotDto snapshot = new WorkflowSnapshotDto(runId, "组局投票", "running", new ArrayList<>());
        List<String> events = new ArrayList<>();
        events.add(streamEmitter.workflow(runId, snapshot.getTitle(), "running"));
        return collectThenCreate(events, snapshot, conversationId, userText, userId, lat, lng, extractOptions(userText));
    }

    public WorkflowResult resume(PendingWorkflowSession session, List<String> extraOptions, String message) {
        List<String> merged = new ArrayList<>(session.getDraftOptions() == null ? List.of() : session.getDraftOptions());
        if (extraOptions != null) {
            merged.addAll(extraOptions);
        }
        merged.addAll(extractOptions(message));
        merged = distinct(merged);
        List<String> events = new ArrayList<>();
        WorkflowSnapshotDto snapshot = session.getSnapshot();
        snapshot.setStatus("running");
        events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "running"));
        return collectThenCreate(
                events,
                snapshot,
                session.getConversationId(),
                session.getUserText(),
                session.getUserId(),
                session.getLat(),
                session.getLng(),
                merged);
    }

    private WorkflowResult collectThenCreate(
            List<String> events,
            WorkflowSnapshotDto snapshot,
            String conversationId,
            String userText,
            Long userId,
            Double lat,
            Double lng,
            List<String> incoming) {
        WorkflowSupport.upsertStep(streamEmitter, events, snapshot, "collect_options", "收集选项", "running", null);
        List<String> options = distinct(incoming);
        if (options.size() < 2) {
            options = distinct(suggestOptions(userId, lat, lng));
        }
        if (options.size() < 2) {
            options = new ArrayList<>(FALLBACK_OPTIONS);
        }

        if (incoming.size() < 2) {
            WorkflowSupport.completeStep(
                    streamEmitter, events, snapshot, "collect_options", "waiting", String.join("、", options));
            snapshot.setStatus("waiting");
            events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "waiting"));
            String reply = "先用这几个选项组局：" + String.join("、", options) + "。点流程卡确认后我就开投票房。";
            events.add(streamEmitter.chunk(reply));
            savePending(snapshot, conversationId, userText, userId, lat, lng, options);
            return new WorkflowResult(events, reply, List.of(), snapshot, true);
        }

        WorkflowSupport.completeStep(
                streamEmitter, events, snapshot, "collect_options", "done", String.join("、", options));
        return createRoom(events, snapshot, userId, topicOf(userText), options);
    }

    private WorkflowResult createRoom(
            List<String> events,
            WorkflowSnapshotDto snapshot,
            Long userId,
            String topic,
            List<String> options) {
        WorkflowSupport.upsertStep(streamEmitter, events, snapshot, "create_room", "创建投票房", "running", null);
        if (userId == null) {
            WorkflowSupport.completeStep(streamEmitter, events, snapshot, "create_room", "failed", "未登录");
            snapshot.setStatus("failed");
            events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "failed"));
            String reply = "创建投票房需要先登录。";
            events.add(streamEmitter.chunk(reply));
            pendingStore.remove(snapshot.getRunId());
            return new WorkflowResult(events, reply, List.of(), snapshot, false);
        }
        try {
            CreateRoomRequest request = new CreateRoomRequest();
            request.setTopic(topic);
            request.setMaxPeople(4);
            request.setDurationMin(30);
            request.setInitialOptions(options);
            CreateRoomResponse room = roomService.createRoom(userId, request);
            ChatActionCardDto card = new ChatActionCardDto("vote_room", 0L, topic, room.getCode());
            WorkflowSupport.completeStep(streamEmitter, events, snapshot, "create_room", "done", "房间 " + room.getCode());
            snapshot.setStatus("done");
            events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "done"));
            events.add(streamEmitter.cards(List.of(card)));
            String reply = "投票房已开好，房间号 " + room.getCode() + "，选项：" + String.join("、", options) + "。把房间号发给朋友就能投。";
            events.add(streamEmitter.chunk(reply));
            pendingStore.remove(snapshot.getRunId());
            return new WorkflowResult(events, reply, List.of(card), snapshot, false);
        } catch (Exception ex) {
            log.warn("create vote room failed: {}", ex.getMessage());
            WorkflowSupport.completeStep(streamEmitter, events, snapshot, "create_room", "failed", "创建失败");
            snapshot.setStatus("failed");
            events.add(streamEmitter.workflow(snapshot.getRunId(), snapshot.getTitle(), "failed"));
            String reply = "投票房暂时开不了，请稍后再试。";
            events.add(streamEmitter.chunk(reply));
            pendingStore.remove(snapshot.getRunId());
            return new WorkflowResult(events, reply, List.of(), snapshot, false);
        }
    }

    private void savePending(
            WorkflowSnapshotDto snapshot,
            String conversationId,
            String userText,
            Long userId,
            Double lat,
            Double lng,
            List<String> options) {
        PendingWorkflowSession session = new PendingWorkflowSession();
        session.setRunId(snapshot.getRunId());
        session.setKind(ChatIntent.VOTE_ROOM);
        session.setConversationId(conversationId);
        session.setUserId(userId);
        session.setWaitingStepId("collect_options");
        session.setUserText(userText);
        session.setTopic(topicOf(userText));
        session.setDraftOptions(options);
        session.setLat(lat);
        session.setLng(lng);
        session.setSnapshot(snapshot);
        pendingStore.put(session);
    }

    private List<String> suggestOptions(Long userId, Double lat, Double lng) {
        if (userId == null || lat == null || lng == null) {
            return FALLBACK_OPTIONS;
        }
        try {
            List<RestaurantDto> nearby = restaurantService.listNearby(userId, lng, lat, null, null);
            List<String> names = nearby.stream()
                    .map(RestaurantDto::getName)
                    .filter(StringUtils::hasText)
                    .limit(3)
                    .toList();
            return names.size() >= 2 ? names : FALLBACK_OPTIONS;
        } catch (Exception ex) {
            return FALLBACK_OPTIONS;
        }
    }

    public static List<String> extractOptions(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        String cleaned = text.trim();
        String[] parts = SPLIT.split(cleaned);
        List<String> options = new ArrayList<>();
        for (String part : parts) {
            String item = part.replaceAll("[?？!！。.\\s]", "")
                    .replace("一起吃", "")
                    .replace("帮我", "")
                    .trim();
            if (item.length() < 2 || item.length() > 12) {
                continue;
            }
            if (STOP.contains(item) || item.contains("投票") || item.contains("组局") || item.contains("房间")) {
                continue;
            }
            if (!options.contains(item)) {
                options.add(item);
            }
        }
        return options;
    }

    static boolean isConfirm(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        String raw = text.trim();
        return raw.contains("确认") || raw.contains("就这些") || raw.contains("用这些")
                || raw.contains("开房") || raw.contains("创建房间") || raw.equals("好的")
                || raw.equals("可以") || raw.equals("行") || raw.contains("开始投票");
    }

    public static boolean isResumeChat(String text) {
        return isConfirm(text) || extractOptions(text).size() >= 2;
    }

    static String topicOf(String text) {
        if (StringUtils.hasText(text) && text.contains("中午")) {
            return "中午吃什么";
        }
        return "今晚吃什么";
    }

    private static List<String> distinct(List<String> items) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String item : items) {
            if (StringUtils.hasText(item)) {
                unique.add(item.trim());
            }
        }
        return new ArrayList<>(unique);
    }
}
