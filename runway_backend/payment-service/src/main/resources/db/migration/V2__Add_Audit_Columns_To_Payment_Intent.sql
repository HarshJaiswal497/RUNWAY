SET @table_name := (
    SELECT CASE
               WHEN EXISTS (
                   SELECT 1
                   FROM information_schema.tables
                   WHERE table_schema = DATABASE() AND table_name = 'payment_intents'
               ) THEN 'payment_intents'
               WHEN EXISTS (
                   SELECT 1
                   FROM information_schema.tables
                   WHERE table_schema = DATABASE() AND table_name = 'payment_intent'
               ) THEN 'payment_intent'
               ELSE NULL
           END
);

SET @alter_sql := IF(
    @table_name IS NULL,
    'SELECT 1',
    CONCAT(
        'ALTER TABLE ', @table_name,
        ' ADD COLUMN IF NOT EXISTS user_id BIGINT NULL,',
        ' ADD COLUMN IF NOT EXISTS created_at DATETIME(6) NULL,',
        ' ADD COLUMN IF NOT EXISTS updated_at DATETIME(6) NULL'
    )
);

PREPARE stmt FROM @alter_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

