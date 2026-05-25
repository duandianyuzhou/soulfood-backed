-- 用户 AI 分层记忆：长期偏好 + 情景经历
CREATE TABLE IF NOT EXISTS sf_ai_user_memory (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    memory_type     VARCHAR(16) NOT NULL,
    content         VARCHAR(512) NOT NULL,
    source          VARCHAR(32) NOT NULL DEFAULT 'auto',
    source_ref      VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT fk_sf_ai_user_memory_user FOREIGN KEY (user_id) REFERENCES sf_user (id) ON DELETE CASCADE,
    CONSTRAINT ck_sf_ai_user_memory_type CHECK (memory_type IN ('long_term', 'episodic'))
);

CREATE INDEX IF NOT EXISTS idx_sf_ai_user_memory_user_type
    ON sf_ai_user_memory (user_id, memory_type, updated_at DESC);

COMMENT ON TABLE sf_ai_user_memory IS 'AI 用户分层记忆：long_term 长期偏好，episodic 近期经历';
COMMENT ON COLUMN sf_ai_user_memory.source IS '来源：auto / user / conversation / activity';
