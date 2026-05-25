-- AI 消息扩展元数据（操作卡片等）
ALTER TABLE sf_ai_chat_message ADD COLUMN IF NOT EXISTS meta_json JSONB;

COMMENT ON COLUMN sf_ai_chat_message.meta_json IS '消息扩展元数据，如 assistant 回复的操作卡片';
