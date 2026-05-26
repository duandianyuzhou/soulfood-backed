SET search_path TO food;

ALTER TABLE sf_friend_message
    ADD COLUMN message_type VARCHAR(32) NOT NULL DEFAULT 'text';

ALTER TABLE sf_friend_message
    ADD COLUMN payload_json TEXT;

COMMENT ON COLUMN sf_friend_message.message_type IS 'text=纯文本 vote_share=投票结果分享卡';

ALTER TABLE sf_friend_message DROP CONSTRAINT IF EXISTS ck_sf_fm_content;

ALTER TABLE sf_friend_message
    ADD CONSTRAINT ck_sf_fm_message_body CHECK (
        (message_type = 'text' AND char_length(trim(content)) > 0)
        OR (
            message_type = 'vote_share'
            AND payload_json IS NOT NULL
            AND char_length(trim(payload_json)) > 2
        )
    );
