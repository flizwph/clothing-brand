-- Проверка существования колонки user_id
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT FROM information_schema.columns 
        WHERE table_name = 'orders' AND column_name = 'user_id'
    ) THEN
        -- Добавление колонки user_id, если она не существует
        ALTER TABLE orders ADD COLUMN user_id int8;
    END IF;
END $$;

-- Обновляем все NULL значения в user_id на 0
UPDATE orders SET user_id = 0 WHERE user_id IS NULL;

-- Изменяем ограничение на столбец user_id
ALTER TABLE orders ALTER COLUMN user_id SET NOT NULL; 