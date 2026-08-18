-- pgvector 知识切片。扩展通常装在 public；无权限时若类型已存在则跳过。
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS vector WITH SCHEMA public;
EXCEPTION
    WHEN insufficient_privilege THEN
        IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'vector') THEN
            RAISE;
        END IF;
    WHEN duplicate_object THEN
        NULL;
END
$$;

SET search_path TO food, public;

CREATE TABLE IF NOT EXISTS sf_rag_chunk (
    id          BIGSERIAL PRIMARY KEY,
    source_type VARCHAR(32) NOT NULL,
    source_id   BIGINT,
    source_key  VARCHAR(64) NOT NULL,
    title       VARCHAR(128),
    content     TEXT NOT NULL,
    embedding   vector(1024) NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_sf_rag_chunk_source_key UNIQUE (source_key),
    CONSTRAINT ck_sf_rag_chunk_source_type CHECK (source_type IN ('recipe', 'faq', 'common'))
);

CREATE INDEX IF NOT EXISTS idx_sf_rag_chunk_hnsw
    ON sf_rag_chunk USING hnsw (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS idx_sf_rag_chunk_type
    ON sf_rag_chunk (source_type);

COMMENT ON TABLE sf_rag_chunk IS 'RAG 知识切片：菜谱 / FAQ / 常识，embedding 由模型计算';
COMMENT ON COLUMN sf_rag_chunk.source_type IS 'recipe | faq | common';
COMMENT ON COLUMN sf_rag_chunk.source_key IS '稳定键，upsert 用，如 faq.room.join、recipe.1';
COMMENT ON COLUMN sf_rag_chunk.embedding IS '向量，维度须与 embedding 模型一致（1024）';
