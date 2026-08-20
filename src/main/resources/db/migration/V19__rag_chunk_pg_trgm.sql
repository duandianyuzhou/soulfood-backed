-- lexical 检索改为 pg_trgm，不再全表拉到 JVM 打 n-gram
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;
EXCEPTION
    WHEN insufficient_privilege THEN
        IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'pg_trgm') THEN
            RAISE;
        END IF;
    WHEN duplicate_object THEN
        NULL;
END
$$;

SET search_path TO food, public;

ALTER TABLE sf_rag_chunk
    ADD COLUMN IF NOT EXISTS search_text TEXT
    GENERATED ALWAYS AS (coalesce(title, '') || ' ' || coalesce(content, '')) STORED;

CREATE INDEX IF NOT EXISTS idx_sf_rag_chunk_title_trgm
    ON sf_rag_chunk USING gin (title gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_sf_rag_chunk_content_trgm
    ON sf_rag_chunk USING gin (content gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_sf_rag_chunk_search_trgm
    ON sf_rag_chunk USING gin (search_text gin_trgm_ops);

COMMENT ON COLUMN sf_rag_chunk.search_text IS 'title+content，供 pg_trgm 检索；由生成列维护';
