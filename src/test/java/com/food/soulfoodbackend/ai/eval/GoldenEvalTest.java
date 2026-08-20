package com.food.soulfoodbackend.ai.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.ai.rag.RagHit;
import com.food.soulfoodbackend.ai.rag.RagLexicalRanker;
import com.food.soulfoodbackend.ai.rag.RagSeedItem;
import com.food.soulfoodbackend.ai.router.ChatIntent;
import com.food.soulfoodbackend.ai.router.RuleIntentRouter;
import com.food.soulfoodbackend.ai.workflow.CookFromFridgeWorkflow;
import com.food.soulfoodbackend.ai.workflow.NearbyEatWorkflow;
import com.food.soulfoodbackend.ai.workflow.VoteRoomWorkflow;
import com.food.soulfoodbackend.domain.entity.SfRagChunk;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 离线黄金集：不打 LLM、不连库。路由走规则；RAG 用 classpath 种子 + 内存近似打分
 *（线上 lexical 为 pg_trgm）；工作流只验 waiting 续跑文案。
 */
class GoldenEvalTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static GoldenFile golden;
    private static List<SfRagChunk> seedChunks;

    @BeforeAll
    static void load() throws Exception {
        try (InputStream eval = resource("ai-eval/golden-eval.json");
             InputStream seeds = resource("rag/seed-faq.json")) {
            golden = MAPPER.readValue(eval, GoldenFile.class);
            List<RagSeedItem> items = MAPPER.readValue(
                    seeds, MAPPER.getTypeFactory().constructCollectionType(List.class, RagSeedItem.class));
            seedChunks = IntStream.range(0, items.size())
                    .mapToObj(i -> toChunk(i + 1L, items.get(i)))
                    .toList();
        }
    }

    @ParameterizedTest(name = "route {0}")
    @MethodSource("routeCases")
    void routeMatchesGolden(RouteCase item) {
        ChatIntent actual = RuleIntentRouter.tryRoute(item.text, item.hasImage, item.hasLocation);
        if ("AMBIGUOUS".equals(item.intent)) {
            assertEquals(null, actual, item.id);
            return;
        }
        assertEquals(ChatIntent.valueOf(item.intent), actual, item.id);
    }

    @ParameterizedTest(name = "rag {0}")
    @MethodSource("ragCases")
    void ragHitsExpectedSeed(RagCase item) {
        List<RagHit> hits = RagLexicalRanker.rank(item.query, seedChunks, item.topK <= 0 ? 3 : item.topK);
        List<String> keys = hits.stream().map(RagHit::getSourceKey).toList();
        assertTrue(
                keys.contains(item.expectSourceKey),
                item.id + " expected " + item.expectSourceKey + " in " + keys);
    }

    @ParameterizedTest(name = "workflow {0}")
    @MethodSource("workflowCases")
    void workflowResumeHelpers(WorkflowCase item) {
        switch (item.kind) {
            case "vote_options" -> assertEquals(item.expectOptions, VoteRoomWorkflow.extractOptions(item.text), item.id);
            case "vote_resume" -> assertEquals(item.expect, VoteRoomWorkflow.isResumeChat(item.text), item.id);
            case "skip_photo" -> assertEquals(item.expect, CookFromFridgeWorkflow.isSkipPhoto(item.text), item.id);
            case "locate_continue" -> assertEquals(item.expect, NearbyEatWorkflow.isLocateContinue(item.text), item.id);
            default -> throw new IllegalArgumentException("unknown kind: " + item.kind);
        }
    }

    private static GoldenFile golden() {
        if (golden == null) {
            try {
                load();
            } catch (Exception ex) {
                throw new IllegalStateException(ex);
            }
        }
        return golden;
    }

    static Stream<RouteCase> routeCases() {
        return golden().route.stream();
    }

    static Stream<RagCase> ragCases() {
        return golden().rag.stream();
    }

    static Stream<WorkflowCase> workflowCases() {
        return golden().workflow.stream();
    }

    private static InputStream resource(String path) {
        InputStream in = GoldenEvalTest.class.getClassLoader().getResourceAsStream(path);
        if (in == null) {
            throw new IllegalStateException("missing classpath resource: " + path);
        }
        return in;
    }

    private static SfRagChunk toChunk(long id, RagSeedItem item) {
        SfRagChunk row = new SfRagChunk();
        row.setId(id);
        row.setSourceType(item.getSourceType());
        row.setSourceKey(item.getSourceKey());
        row.setTitle(item.getTitle());
        row.setContent(item.getContent());
        return row;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GoldenFile {
        public List<RouteCase> route;
        public List<RagCase> rag;
        public List<WorkflowCase> workflow;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RouteCase {
        public String id;
        public String text;
        public boolean hasImage;
        public boolean hasLocation;
        public String intent;

        @Override
        public String toString() {
            return id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class RagCase {
        public String id;
        public String query;
        public String expectSourceKey;
        public int topK;

        @Override
        public String toString() {
            return id;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    static class WorkflowCase {
        public String id;
        public String kind;
        public String text;
        public Boolean expect;
        public List<String> expectOptions;

        @Override
        public String toString() {
            return id;
        }
    }
}
