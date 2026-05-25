-- 账号密码登录 + 用户邀请码
SET search_path TO food;

ALTER TABLE sf_user
    ADD COLUMN IF NOT EXISTS username VARCHAR(64),
    ADD COLUMN IF NOT EXISTS password_hash VARCHAR(128),
    ADD COLUMN IF NOT EXISTS invite_code VARCHAR(16);

ALTER TABLE sf_user ALTER COLUMN open_id DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sf_user_username
    ON sf_user (username)
    WHERE username IS NOT NULL AND deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_sf_user_invite_code
    ON sf_user (invite_code)
    WHERE invite_code IS NOT NULL;

COMMENT ON COLUMN sf_user.username IS '账号登录用户名';
COMMENT ON COLUMN sf_user.password_hash IS 'BCrypt 密码哈希';
COMMENT ON COLUMN sf_user.invite_code IS '个人邀请码';
