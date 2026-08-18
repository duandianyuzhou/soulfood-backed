package com.food.soulfoodbackend.ai.rag;

import com.food.soulfoodbackend.domain.entity.SfRagChunk;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagLexicalRankerTest {

    @Test
    void ranksJoinRoomFaqFirst() {
        SfRagChunk join = chunk(1L, "faq.room.join", "怎么加入别人的房间", "输入 6 位房间号或扫描二维码");
        SfRagChunk leftover = chunk(2L, "common.leftover.rice", "隔夜米饭怎么处理", "隔夜米饭建议尽快冷藏");
        List<RagHit> hits = RagLexicalRanker.rank("怎么加入房间", List.of(leftover, join), 3);
        assertEquals("faq.room.join", hits.get(0).getSourceKey());
        assertTrue(hits.get(0).getDistance() < hits.get(hits.size() - 1).getDistance()
                || hits.size() == 1);
    }

    private static SfRagChunk chunk(Long id, String key, String title, String content) {
        SfRagChunk row = new SfRagChunk();
        row.setId(id);
        row.setSourceKey(key);
        row.setSourceType(key.startsWith("faq") ? "faq" : "common");
        row.setTitle(title);
        row.setContent(title + "。" + content);
        return row;
    }
}
