package com.food.soulfoodbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.food.soulfoodbackend.ai.rag.RagHit;
import com.food.soulfoodbackend.domain.entity.SfRagChunk;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SfRagChunkMapper extends BaseMapper<SfRagChunk> {

    @Insert("""
            INSERT INTO sf_rag_chunk (source_type, source_id, source_key, title, content, embedding)
            VALUES (#{sourceType}, #{sourceId}, #{sourceKey}, #{title}, #{content}, CAST(#{embedding} AS public.vector))
            ON CONFLICT (source_key) DO UPDATE SET
                source_type = EXCLUDED.source_type,
                source_id = EXCLUDED.source_id,
                title = EXCLUDED.title,
                content = EXCLUDED.content,
                embedding = EXCLUDED.embedding
            """)
    int upsert(
            @Param("sourceType") String sourceType,
            @Param("sourceId") Long sourceId,
            @Param("sourceKey") String sourceKey,
            @Param("title") String title,
            @Param("content") String content,
            @Param("embedding") String embedding);

    @Select("""
            SELECT id, source_type AS sourceType, source_id AS sourceId, source_key AS sourceKey,
                   title, content, (embedding <=> CAST(#{embedding} AS public.vector)) AS distance
            FROM sf_rag_chunk
            ORDER BY embedding <=> CAST(#{embedding} AS public.vector)
            LIMIT #{limit}
            """)
    List<RagHit> searchNearest(@Param("embedding") String embedding, @Param("limit") int limit);
}