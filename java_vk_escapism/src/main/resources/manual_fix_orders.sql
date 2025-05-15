-- Этот скрипт нужно выполнить через pgAdmin или другой SQL клиент
-- Подключитесь к базе данных vk_escapism и выполните следующие запросы

-- 1. Проверить наличие колонки user_id
SELECT column_name, data_type, is_nullable 
FROM information_schema.columns 
WHERE table_name = 'orders' AND column_name = 'user_id';

-- 2. Если колонка существует, но может содержать NULL, обновите значения:
UPDATE orders SET user_id = 0 WHERE user_id IS NULL;

-- 3. Если колонки не существует, создайте ее:
-- ALTER TABLE orders ADD COLUMN user_id int8;
-- UPDATE orders SET user_id = 0;

-- 4. Установите ограничение NOT NULL:
-- ALTER TABLE orders ALTER COLUMN user_id SET NOT NULL;

-- 5. Проверка результата:
SELECT COUNT(*) FROM orders WHERE user_id IS NULL; 