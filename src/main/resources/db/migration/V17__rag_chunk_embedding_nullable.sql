-- embedding 改为可空：无向量模型时只存正文，用关键词检索
SET search_path TO food, public;

ALTER TABLE sf_rag_chunk
    ALTER COLUMN embedding DROP NOT NULL;

COMMENT ON COLUMN sf_rag_chunk.embedding IS '可空。未配置 embedding 模型时走关键词检索；有模型后再回填';
