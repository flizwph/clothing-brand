-- Инициализация базы данных для VK Escapism Bot

-- Создание таблицы пользователей
CREATE TABLE IF NOT EXISTS users (
    vk_id BIGINT PRIMARY KEY,
    last_state VARCHAR(50),
    last_interaction TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Создание таблицы заказов
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    user_vk_id BIGINT NOT NULL,
    order_data TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'order_confirmed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_vk_id) REFERENCES users(vk_id)
);

-- Создание индексов
CREATE INDEX IF NOT EXISTS idx_users_vk_id ON users(vk_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_vk_id ON orders(user_vk_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);

-- Комментарии к таблицам и полям
COMMENT ON TABLE users IS 'Таблица пользователей ВК бота';
COMMENT ON COLUMN users.vk_id IS 'ID пользователя ВКонтакте';
COMMENT ON COLUMN users.last_state IS 'Последнее состояние пользователя в системе';
COMMENT ON COLUMN users.last_interaction IS 'Время последнего взаимодействия';

COMMENT ON TABLE orders IS 'Таблица заказов';
COMMENT ON COLUMN orders.id IS 'Уникальный ID заказа';
COMMENT ON COLUMN orders.user_vk_id IS 'ID пользователя ВКонтакте, создавшего заказ';
COMMENT ON COLUMN orders.order_data IS 'Данные заказа в текстовом формате';
COMMENT ON COLUMN orders.status IS 'Статус заказа';
COMMENT ON COLUMN orders.created_at IS 'Время создания заказа';
COMMENT ON COLUMN orders.updated_at IS 'Время последнего обновления заказа';