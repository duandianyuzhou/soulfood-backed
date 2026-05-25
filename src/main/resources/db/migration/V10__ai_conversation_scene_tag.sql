-- 会话场景标签：聚餐 / 减脂 / 探店 / 日常
ALTER TABLE sf_ai_conversation ADD COLUMN IF NOT EXISTS scene_tag VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_sf_ai_conversation_scene_tag
    ON sf_ai_conversation (user_id, scene_tag);

COMMENT ON COLUMN sf_ai_conversation.scene_tag IS '会话场景标签：聚餐、减脂、探店、日常等';
