package com.food.soulfoodbackend.ai.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.soulfoodbackend.domain.entity.SfRagChunk;
import com.food.soulfoodbackend.domain.entity.SfRecipe;
import com.food.soulfoodbackend.mapper.SfRagChunkMapper;
import com.food.soulfoodbackend.mapper.SfRecipeMapper;
import com.food.soulfoodbackend.service.JsonStrings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagIngestService {

    private final SfRagChunkMapper chunkMapper;
    private final SfRecipeMapper recipeMapper;
    private final EmbeddingClient embeddingClient;
    private final ObjectMapper objectMapper;

    public IngestReport ingestAll() {
        int seeds = ingestSeeds();
        int recipes = ingestRecipes();
        log.info("RAG ingest done: seedsUpserted={}, recipesUpserted={}", seeds, recipes);
        return new IngestReport(seeds, recipes);
    }

    public int ingestSeeds() {
        List<RagSeedItem> seeds = loadSeeds();
        return upsertChanged(seeds.stream().map(this::toDoc).toList());
    }

    public int ingestRecipes() {
        List<SfRecipe> recipes = recipeMapper.selectList(new LambdaQueryWrapper<SfRecipe>()
                .orderByAsc(SfRecipe::getId));
        List<RagDocument> docs = new ArrayList<>();
        for (SfRecipe recipe : recipes) {
            docs.add(new RagDocument(
                    "recipe",
                    recipe.getId(),
                    "recipe." + recipe.getId(),
                    recipe.getName(),
                    formatRecipe(recipe)));
        }
        return upsertChanged(docs);
    }

    private int upsertChanged(List<RagDocument> docs) {
        if (docs.isEmpty()) {
            return 0;
        }
        Map<String, SfRagChunk> existing = chunkMapper.selectList(new LambdaQueryWrapper<SfRagChunk>()
                        .in(SfRagChunk::getSourceKey, docs.stream().map(RagDocument::sourceKey).toList()))
                .stream()
                .collect(Collectors.toMap(SfRagChunk::getSourceKey, Function.identity(), (a, b) -> a));

        List<RagDocument> changed = docs.stream()
                .filter(doc -> {
                    SfRagChunk row = existing.get(doc.sourceKey());
                    return row == null || !Objects.equals(row.getContent(), doc.content());
                })
                .toList();
        if (changed.isEmpty()) {
            return 0;
        }
        List<float[]> vectors = embeddingClient.embedAll(changed.stream().map(RagDocument::content).toList());
        for (int i = 0; i < changed.size(); i++) {
            RagDocument doc = changed.get(i);
            chunkMapper.upsert(
                    doc.sourceType(),
                    doc.sourceId(),
                    doc.sourceKey(),
                    doc.title(),
                    doc.content(),
                    embeddingClient.toPgVector(vectors.get(i)));
        }
        return changed.size();
    }

    private List<RagSeedItem> loadSeeds() {
        try (InputStream in = new ClassPathResource("rag/seed-faq.json").getInputStream()) {
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new IllegalStateException("无法读取 rag/seed-faq.json", ex);
        }
    }

    private RagDocument toDoc(RagSeedItem item) {
        if (!StringUtils.hasText(item.getSourceKey()) || !StringUtils.hasText(item.getContent())) {
            throw new IllegalStateException("种子缺少 sourceKey 或 content");
        }
        return new RagDocument(
                item.getSourceType(),
                null,
                item.getSourceKey(),
                item.getTitle(),
                item.getTitle() + "。" + item.getContent());
    }

    private static String formatRecipe(SfRecipe recipe) {
        List<String> ingredients = JsonStrings.parseStringList(recipe.getIngredientsJson());
        List<String> steps = JsonStrings.parseStringList(recipe.getStepsJson());
        return """
                菜名：%s
                分类：%s
                难度：%s
                耗时：%s 分钟
                简介：%s
                食材：%s
                步骤：%s
                """.formatted(
                nullToEmpty(recipe.getName()),
                nullToEmpty(recipe.getCategory()),
                nullToEmpty(recipe.getDifficulty()),
                recipe.getDurationMin() == null ? "-" : recipe.getDurationMin(),
                nullToEmpty(recipe.getSummary()),
                String.join("、", ingredients),
                String.join("；", steps));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record IngestReport(int seedsUpserted, int recipesUpserted) {
    }

    private record RagDocument(
            String sourceType,
            Long sourceId,
            String sourceKey,
            String title,
            String content) {
    }
}
