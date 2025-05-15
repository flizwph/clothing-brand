# Контроллер пользователей

## Общая информация
Контроллер пользователей предоставляет API для управления профилем пользователя, включая получение и обновление персональных данных.

- **Базовый путь**: `/api/users`
- **Реализация**: `UserController.java`

## Эндпоинты

### Получение информации о текущем пользователе
Возвращает информацию о текущем аутентифицированном пользователе.

- **URL**: `/api/users/me`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ (200 OK)**:
```json
{
  "id": 1,
  "username": "test_user",
  "email": "user@example.com",
  "phoneNumber": "+79123456789",
  "role": "ROLE_USER",
  "active": true,
  "verified": true,
  "telegramUsername": "telegram_user",
  "discordUsername": "discord_user",
  "vkUsername": "vk_user",
  "linkedDiscord": false,
  "linkedVkontakte": false,
  "createdAt": "2025-04-10T15:30:00",
  "updatedAt": "2025-05-01T10:15:00",
  "lastLogin": "2025-05-01T18:20:00"
}
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован
- `404 Not Found` - Пользователь не найден

### Проверка верификации в Telegram
Проверяет, прошел ли пользователь верификацию в Telegram.

- **URL**: `/api/users/is-verified`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ (200 OK)**:
```json
{
  "isVerified": true
}
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован

### Обновление профиля пользователя
Обновляет данные профиля текущего пользователя.

- **URL**: `/api/users/update`
- **Метод**: `PUT`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Тело запроса**:
```json
{
  "newUsername": "new_username",
  "email": "new_email@example.com",
  "phoneNumber": "+79876543210"
}
```

**Примечание**: Все поля в запросе опциональны. Можно обновить любое из них или все вместе.

**Успешный ответ (200 OK)**:
```json
{
  "message": "User profile updated successfully"
}
```

**Ошибки**:
- `400 Bad Request` - Не предоставлены данные для обновления
- `401 Unauthorized` - Пользователь не авторизован
- `409 Conflict` - Имя пользователя уже занято

### Изменение пароля пользователя
Изменяет пароль текущего пользователя.

- **URL**: `/api/users/change-password`
- **Метод**: `PUT`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Тело запроса**:
```json
{
  "oldPassword": "current_password",
  "newPassword": "new_password"
}
```

**Успешный ответ (200 OK)**:
```json
{
  "message": "Password changed successfully"
}
```

**Ошибки**:
- `400 Bad Request` - Отсутствуют необходимые поля или неверный текущий пароль
- `401 Unauthorized` - Пользователь не авторизован

### Получение контактной информации пользователя
Возвращает только контактную информацию текущего пользователя.

- **URL**: `/api/users/contacts`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ (200 OK)**:
```json
{
  "email": "user@example.com",
  "phoneNumber": "+79123456789"
}
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован 