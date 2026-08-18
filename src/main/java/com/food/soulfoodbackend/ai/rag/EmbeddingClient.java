package com.food.soulfoodbackend.ai.rag;

import com.food.soulfoodbackend.config.AiRagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmbeddingClient {

    private static final int BATCH = 32;

    private final EmbeddingModel embeddingModel;
    private final AiRagProperties properties;

    public float[] embed(String text) {
        List<float[]> all = embedAll(List.of(text));
        return all.get(0);
    }

    public List<float[]> embedAll(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }
        List<float[]> out = new ArrayList<>(texts.size());
        for (int i = 0; i < texts.size(); i += BATCH) {
            List<String> batch = texts.subList(i, Math.min(i + BATCH, texts.size()));
            var response = embeddingModel.call(new EmbeddingRequest(batch, options()));
            if (response.getResults().size() != batch.size()) {
                throw new IllegalStateException("embedding 条数与输入不一致");
            }
            for (var result : response.getResults()) {
                float[] vector = toFloatArray(result.getOutput());
                assertDims(vector);
                out.add(vector);
            }
        }
        return out;
    }

    public String toPgVector(float[] vector) {
        assertDims(vector);
        StringBuilder sb = new StringBuilder(vector.length * 8);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(vector[i]);
        }
        sb.append(']');
        return sb.toString();
    }

    private OpenAiEmbeddingOptions options() {
        return OpenAiEmbeddingOptions.builder()
                .model(properties.getEmbeddingModel())
                .dimensions(properties.getEmbeddingDims())
                .build();
    }

    private void assertDims(float[] vector) {
        int expected = properties.getEmbeddingDims();
        if (vector == null || vector.length != expected) {
            throw new IllegalStateException(String.format(
                    Locale.ROOT,
                    "embedding 维度为 %s，配置/DDL 为 %d，禁止写入",
                    vector == null ? "null" : vector.length,
                    expected));
        }
    }

    private static float[] toFloatArray(Object output) {
        if (output instanceof float[] floats) {
            return floats;
        }
        if (output instanceof List<?> list) {
            float[] arr = new float[list.size()];
            for (int i = 0; i < list.size(); i++) {
                arr[i] = ((Number) list.get(i)).floatValue();
            }
            return arr;
        }
        throw new IllegalStateException("无法解析 embedding 输出: " + output.getClass());
    }
}
