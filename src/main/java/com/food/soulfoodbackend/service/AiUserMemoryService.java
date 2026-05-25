package com.food.soulfoodbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.food.soulfoodbackend.common.BusinessException;
import com.food.soulfoodbackend.common.ErrorCode;
import com.food.soulfoodbackend.domain.entity.SfActivityRecord;
import com.food.soulfoodbackend.domain.entity.SfAiUserMemory;
import com.food.soulfoodbackend.dto.ai.AiUserMemoryDto;
import com.food.soulfoodbackend.mapper.SfActivityRecordMapper;
import com.food.soulfoodbackend.mapper.SfAiUserMemoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiUserMemoryService {

    public static final String TYPE_LONG_TERM = "long_term";
    public static final String TYPE_EPISODIC = "episodic";

    private static final int MAX_LONG_TERM = 12;
    private static final int MAX_EPISODIC = 10;
    private static final int CONTEXT_LONG_TERM = 8;
    private static final int CONTEXT_EPISODIC = 6;
    private static final int CONTENT_MAX = 512;
    private static final DateTimeFormatter EPISODIC_TIME = DateTimeFormatter.ofPattern("M月d日");

    private final SfAiUserMemoryMapper memoryMapper;
    private final SfActivityRecordMapper activityRecordMapper;

    public List<AiUserMemoryDto> listForUser(Long userId, String memoryType) {
        if (userId == null) {
            return List.of();
        }
        LambdaQueryWrapper<SfAiUserMemory> wrapper = new LambdaQueryWrapper<SfAiUserMemory>()
                .eq(SfAiUserMemory::getUserId, userId)
                .orderByDesc(SfAiUserMemory::getUpdatedAt);
        if (StringUtils.hasText(memoryType)) {
            wrapper.eq(SfAiUserMemory::getMemoryType, memoryType.trim());
        }
        return memoryMapper.selectList(wrapper).stream().map(this::toDto).toList();
    }

    @Transactional
    public AiUserMemoryDto addUserMemory(Long userId, String content, String memoryType) {
        assertUser(userId);
        String type = normalizeType(memoryType);
        return toDto(upsertMemory(userId, type, truncate(content.trim()), "user", null));
    }

    @Transactional
    public void deleteMemory(Long userId, Long memoryId) {
        assertUser(userId);
        SfAiUserMemory row = memoryMapper.selectById(memoryId);
        if (row == null || !userId.equals(row.getUserId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "记忆不存在");
        }
        memoryMapper.deleteById(memoryId);
    }

    @Transactional
    public void addExtractedMemories(Long userId, List<String> contents, String source, String sourceRef) {
        if (userId == null || contents == null || contents.isEmpty()) {
            return;
        }
        for (String content : contents) {
            if (!StringUtils.hasText(content)) {
                continue;
            }
            upsertMemory(userId, TYPE_LONG_TERM, truncate(content.trim()), source, sourceRef);
        }
        trimOverflow(userId, TYPE_LONG_TERM, MAX_LONG_TERM);
    }

    public String buildMemoryContextBlock(Long userId) {
        if (userId == null) {
            return "";
        }
        syncEpisodicFromActivity(userId);
        List<SfAiUserMemory> longTerm = memoryMapper.selectList(new LambdaQueryWrapper<SfAiUserMemory>()
                .eq(SfAiUserMemory::getUserId, userId)
                .eq(SfAiUserMemory::getMemoryType, TYPE_LONG_TERM)
                .orderByDesc(SfAiUserMemory::getUpdatedAt)
                .last("LIMIT " + CONTEXT_LONG_TERM));
        List<SfAiUserMemory> episodic = memoryMapper.selectList(new LambdaQueryWrapper<SfAiUserMemory>()
                .eq(SfAiUserMemory::getUserId, userId)
                .eq(SfAiUserMemory::getMemoryType, TYPE_EPISODIC)
                .orderByDesc(SfAiUserMemory::getUpdatedAt)
                .last("LIMIT " + CONTEXT_EPISODIC));
        if (longTerm.isEmpty() && episodic.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("\n\n【AI 记忆】");
        if (!longTerm.isEmpty()) {
            sb.append("\n长期偏好与习惯：");
            for (SfAiUserMemory row : longTerm) {
                sb.append("\n- ").append(row.getContent());
            }
        }
        if (!episodic.isEmpty()) {
            sb.append("\n近期经历：");
            for (SfAiUserMemory row : episodic) {
                sb.append("\n- ").append(row.getContent());
            }
        }
        sb.append("\n请结合以上记忆个性化回答，与用户已知偏好保持一致。");
        return sb.toString();
    }

    @Transactional
    public void syncEpisodicFromActivity(Long userId) {
        if (userId == null) {
            return;
        }
        List<SfActivityRecord> records = activityRecordMapper.selectList(new LambdaQueryWrapper<SfActivityRecord>()
                .eq(SfActivityRecord::getUserId, userId)
                .in(SfActivityRecord::getRecordType, "vote", "eat", "favorite")
                .orderByDesc(SfActivityRecord::getOccurredAt)
                .last("LIMIT 8"));
        for (SfActivityRecord record : records) {
            String content = formatActivityMemory(record);
            if (!StringUtils.hasText(content)) {
                continue;
            }
            String ref = "activity:" + record.getId();
            if (existsBySourceRef(userId, TYPE_EPISODIC, ref)) {
                continue;
            }
            upsertMemory(userId, TYPE_EPISODIC, content, "activity", ref);
        }
        trimOverflow(userId, TYPE_EPISODIC, MAX_EPISODIC);
    }

    private SfAiUserMemory upsertMemory(
            Long userId, String memoryType, String content, String source, String sourceRef) {
        SfAiUserMemory existing = findSimilar(userId, memoryType, content);
        OffsetDateTime now = OffsetDateTime.now();
        if (existing != null) {
            existing.setContent(content);
            existing.setSource(source);
            if (sourceRef != null) {
                existing.setSourceRef(sourceRef);
            }
            existing.setUpdatedAt(now);
            memoryMapper.updateById(existing);
            return existing;
        }
        SfAiUserMemory row = new SfAiUserMemory();
        row.setUserId(userId);
        row.setMemoryType(memoryType);
        row.setContent(content);
        row.setSource(source);
        row.setSourceRef(sourceRef);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        memoryMapper.insert(row);
        return row;
    }

    private SfAiUserMemory findSimilar(Long userId, String memoryType, String content) {
        List<SfAiUserMemory> rows = memoryMapper.selectList(new LambdaQueryWrapper<SfAiUserMemory>()
                .eq(SfAiUserMemory::getUserId, userId)
                .eq(SfAiUserMemory::getMemoryType, memoryType)
                .orderByDesc(SfAiUserMemory::getUpdatedAt)
                .last("LIMIT 50"));
        String normalized = content.toLowerCase();
        for (SfAiUserMemory row : rows) {
            if (row.getContent() == null) {
                continue;
            }
            String existing = row.getContent().toLowerCase();
            if (existing.equals(normalized) || existing.contains(normalized) || normalized.contains(existing)) {
                return row;
            }
        }
        return null;
    }

    private boolean existsBySourceRef(Long userId, String memoryType, String sourceRef) {
        return memoryMapper.selectCount(new LambdaQueryWrapper<SfAiUserMemory>()
                .eq(SfAiUserMemory::getUserId, userId)
                .eq(SfAiUserMemory::getMemoryType, memoryType)
                .eq(SfAiUserMemory::getSourceRef, sourceRef)) > 0;
    }

    private void trimOverflow(Long userId, String memoryType, int max) {
        List<SfAiUserMemory> rows = memoryMapper.selectList(new LambdaQueryWrapper<SfAiUserMemory>()
                .eq(SfAiUserMemory::getUserId, userId)
                .eq(SfAiUserMemory::getMemoryType, memoryType)
                .orderByDesc(SfAiUserMemory::getUpdatedAt));
        if (rows.size() <= max) {
            return;
        }
        for (int i = max; i < rows.size(); i++) {
            memoryMapper.deleteById(rows.get(i).getId());
        }
    }

    private String formatActivityMemory(SfActivityRecord record) {
        String time = record.getOccurredAt() != null
                ? EPISODIC_TIME.format(record.getOccurredAt())
                : "近期";
        String title = record.getTitle() != null ? record.getTitle().trim() : "";
        String summary = record.getSummary() != null ? record.getSummary().trim() : "";
        if (title.isBlank()) {
            return "";
        }
        return switch (record.getRecordType()) {
            case "vote" -> time + " " + title + (summary.isBlank() ? "" : "（" + summary + "）");
            case "eat" -> time + " 标记想去「" + summary + "」";
            case "favorite" -> time + " " + title;
            default -> time + " " + title;
        };
    }

    private String normalizeType(String memoryType) {
        if (TYPE_EPISODIC.equals(memoryType)) {
            return TYPE_EPISODIC;
        }
        return TYPE_LONG_TERM;
    }

    private AiUserMemoryDto toDto(SfAiUserMemory row) {
        return new AiUserMemoryDto(
                row.getId(),
                row.getMemoryType(),
                row.getContent(),
                row.getSource(),
                row.getUpdatedAt());
    }

    private static void assertUser(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
    }

    private static String truncate(String text) {
        if (text.length() <= CONTENT_MAX) {
            return text;
        }
        return text.substring(0, CONTENT_MAX - 1) + "…";
    }
}
