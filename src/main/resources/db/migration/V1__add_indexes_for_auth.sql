-- Индекс для быстрого поиска пользователя по имени (улучшает производительность авторизации)
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- Индекс для быстрого поиска refresh токенов по user_id
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);

-- Индекс для быстрого поиска refresh токенов по значению токена
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);

-- Индекс для оптимизации запросов, использующих last_login
CREATE INDEX IF NOT EXISTS idx_users_last_login ON users(last_login); 