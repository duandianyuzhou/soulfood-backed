-- 待确认工作流（waiting 步骤），重启后仍可续跑
SET search_path TO food, public;

CREATE TABLE IF NOT EXISTS sf_ai_workflow_pending (
    run_id            VARCHAR(64) PRIMARY KEY,
    conversation_id   VARCHAR(64) NOT NULL,
    user_id           BIGINT,
    kind              VARCHAR(32) NOT NULL,
    waiting_step_id   VARCHAR(64) NOT NULL,
    payload_json      JSONB NOT NULL,
    expires_at        TIMESTAMPTZ NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_sf_ai_workflow_pending_conv
    ON sf_ai_workflow_pending (conversation_id);

CREATE INDEX IF NOT EXISTS idx_sf_ai_workflow_pending_exp
    ON sf_ai_workflow_pending (expires_at);

COMMENT ON TABLE sf_ai_workflow_pending IS '聊天流程 waiting 态，供续跑 API 与下一轮对话恢复';
