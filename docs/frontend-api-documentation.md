# Документация API для фронтенд-разработчика

## Авторизация и аутентификация

### Регистрация пользователя
```
POST /api/auth/register
```

**Тело запроса:**
```json
{
  "username": "user123",
  "password": "password123",
  "email": "user@example.com"
}
```

**Ответ:**
```json
{
  "message": "User registered successfully",
  "verificationCode": "a1b2c3d4"
}
```

### Вход в систему
```
POST /api/auth/login
```

**Тело запроса:**
```json
{
  "username": "user123",
  "password": "password123"
}
```

**Ответ (успешный):**
```json
{
  "message": "Login successful",
  "accessToken": "eyJhbGciOiJIU...",
  "refreshToken": "eyJhbGciOiJIU..."
}
```

**Ответ (не верифицирован):**
```json
{
  "message": "Account not verified. Use this code in Telegram bot:",
  "verificationCode": "a1b2c3d4"
}
```

## Пользовательские данные

### Получение данных текущего пользователя
```
GET /api/users/me
```

**Заголовки:**
```
Authorization: Bearer {access_token}
```

**Ответ:**
```json
{
  "id": 1,
  "username": "user123",
  "role": "ROLE_USER",
  "active": true,
  "telegramId": 123456789,
  "telegramUsername": "telegram_user",
  "discordId": 123456789,
  "discordUsername": "discord_user",
  "email": "user@example.com",
  "phoneNumber": "+79001234567",
  "verified": true,
  "linkedDiscord": true,
  "createdAt": "2023-01-01T12:00:00"
}
```

### Обновление профиля
```
PUT /api/users/update
```

**Заголовки:**
```
Authorization: Bearer {access_token}
```

**Тело запроса:**
```json
{
  "newUsername": "newuser123",
  "email": "newemail@example.com",
  "phoneNumber": "+79009876543"
}
```

**Ответ:**
```json
{
  "message": "User profile updated successfully"
}
```

### Проверка верификации Telegram
```
GET /api/users/is-verified
```

**Заголовки:**
```
Authorization: Bearer {access_token}
```

**Ответ:**
```json
{
  "isVerified": true
}
```

## Discord интеграция

### Генерация кода верификации для Discord
```
GET /api/discord/generate-code
```

**Заголовки:**
```
Authorization: Bearer {access_token}
```

**Ответ:**
```json
{
  "code": "a1b2c3d4",
  "message": "Используйте этот код в Discord боте с командой !link"
}
```

### Проверка статуса верификации Discord
```
POST /api/discord/check-status
```

**Заголовки:**
```
Content-Type: application/json
X-API-KEY: {discord_api_secret_key}
```

**Тело запроса:**
```json
{
  "discordId": "123456789",
  "discordUsername": "discord_user"
}
```

**Ответ (пользователь верифицирован):**
```json
{
  "success": true,
  "message": "Ваш аккаунт успешно привязан к сайту! Имя пользователя: username",
  "username": "username",
  "telegramHandle": "@telegram_user"
}
```

**Ответ (пользователь не верифицирован):**
```json
{
  "success": false,
  "message": "Аккаунт не найден. Пожалуйста, завершите верификацию на сайте."
}
```

## Компоненты фронтенда

### DiscordVerification

Компонент для привязки Discord-аккаунта. Позволяет пользователю:
1. Сгенерировать код верификации
2. Скопировать код для последующей отправки Discord-боту
3. Получить инструкции по привязке аккаунта

#### Пример использования:
```jsx
import DiscordVerification from './components/DiscordVerification';

function Profile() {
  return (
    <div>
      <h1>Профиль пользователя</h1>
      <DiscordVerification />
    </div>
  );
}
```

## Процесс верификации через Discord

1. Пользователь может привязать Discord одним из двух способов:
   - На сайте через компонент DiscordVerification
   - Через Telegram бота с помощью команды `/linkDiscord`

2. При использовании Telegram бота:
   - Пользователь отправляет команду `/linkDiscord`
   - Бот генерирует уникальный код и отправляет его пользователю с инструкциями
   - Пользователь использует этот код в Discord боте для привязки аккаунта

3. При использовании сайта:
   - Пользователь нажимает кнопку "Сгенерировать код верификации" на странице
   - Запрос отправляется на бэкенд, который генерирует уникальный код
   - Код отображается пользователю в интерфейсе
   
4. Дальнейшие шаги одинаковы для обоих способов:
   - Пользователь отправляет боту команду `!link КОД` (например: `!link a1b2c3d4`)
   - Или отправляет `!link`, а затем код верификации отдельным сообщением
   - Бот отправляет запрос на бэкенд для проверки кода
   - Бэкенд привязывает Discord-аккаунт и отправляет результат
   - Бот информирует пользователя о результате верификации

## Структура запросов для Discord бота

### Проверка кода верификации
```
POST /api/discord/verify
```

**Заголовки:**
```
Content-Type: application/json
X-API-KEY: {discord_api_secret_key}
```

**Тело запроса:**
```json
{
  "code": "a1b2c3d4",
  "discordId": "123456789",
  "discordUsername": "discord_user"
}
```

**Ответ (успешный):**
```json
{
  "success": true,
  "message": "Discord аккаунт успешно привязан"
}
```

**Ответ (ошибка):**
```json
{
  "success": false,
  "message": "Неверный код верификации"
}
```

## Рекомендации для фронтенд-разработчика

1. Используйте компонент `DiscordVerification` для интеграции привязки Discord на страницу профиля
2. Добавьте индикатор связанных аккаунтов в профиле пользователя
3. Используйте токен JWT во всех запросах через заголовок `Authorization`
4. Обработайте случай, когда пользователь не верифицирован при входе

## Обработка ошибок

Все API-запросы могут вернуть следующие статусы:
- `200` - Успешное выполнение
- `400` - Ошибка в запросе
- `401` - Не авторизован
- `403` - Доступ запрещен
- `500` - Внутренняя ошибка сервера

При получении ошибок рекомендуется отображать пользователю соответствующие сообщения и, при необходимости, перенаправлять на страницу входа. 