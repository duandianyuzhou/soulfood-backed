-- 兼容早期误用 soulfood schema 的环境，统一为 food
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.schemata WHERE schema_name = 'soulfood'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.schemata WHERE schema_name = 'food'
    ) THEN
        ALTER SCHEMA soulfood RENAME TO food;
    END IF;
END $$;
