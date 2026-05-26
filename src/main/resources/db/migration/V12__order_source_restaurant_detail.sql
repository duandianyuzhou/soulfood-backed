ALTER TABLE sf_order ADD COLUMN IF NOT EXISTS source VARCHAR(16) NOT NULL DEFAULT 'manual';
COMMENT ON COLUMN sf_order.source IS '订单来源：manual 用户记账 / demo 示例数据';

ALTER TABLE sf_restaurant ADD COLUMN IF NOT EXISTS phone VARCHAR(32);
ALTER TABLE sf_restaurant ADD COLUMN IF NOT EXISTS photo_url VARCHAR(512);

UPDATE sf_order SET source = 'demo'
WHERE restaurant_name IN ('海底捞（万象城店）', '喜茶（万象城店）', '麦当劳（万象城店）')
  AND source = 'manual';
