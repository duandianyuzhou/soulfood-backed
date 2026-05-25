-- 高德 POI 外部 ID 唯一索引，用于 upsert
CREATE UNIQUE INDEX IF NOT EXISTS uk_sf_restaurant_external_id
    ON sf_restaurant (external_id)
    WHERE external_id IS NOT NULL AND external_id <> '';
