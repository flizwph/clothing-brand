-- Обновление существующих строк с NULL в user_id
UPDATE orders SET user_id = 0 WHERE user_id IS NULL;

-- Создание временной таблицы для временного хранения данных
CREATE TABLE temp_orders AS SELECT * FROM orders;

-- Удаление данных из оригинальной таблицы
DELETE FROM orders;

-- Изменение ограничения на столбец user_id
ALTER TABLE orders ALTER COLUMN user_id SET NOT NULL;

-- Копирование данных обратно в оригинальную таблицу
INSERT INTO orders SELECT * FROM temp_orders;

-- Удаление временной таблицы
DROP TABLE temp_orders; 