# Контроллер аутентификации

## Общая информация
Контроллер аутентификации предоставляет API для регистрации и авторизации пользователей, а также управления токенами доступа.

- **Базовый путь**: `/api/auth/v2`
- **Реализация**: `AuthControllerCQRS.java`

## Эндпоинты

### Регистрация пользователя
Регистрирует нового пользователя в системе.

- **URL**: `/api/auth/v2/register`
- **Метод**: `POST`
- **Требует авторизации**: Нет

**Тело запроса**:
```json
{
  "username": "test_user",
  "password": "password123"
}
```

**Успешный ответ (200 OK)**:
```json
{
  "message": "User registered successfully",
  "verificationCode": "a1b2c3d4"
}
```

**Ошибки**:
- `409 Conflict` - Пользователь с таким именем уже существует
- `500 Internal Server Error` - Внутренняя ошибка сервера

**Примечание**:
После регистрации пользователь получает код верификации, который необходимо использовать в Telegram-боте для активации аккаунта.

### Авторизация пользователя
Выполняет вход пользователя в систему и выдает токены доступа.

- **URL**: `/api/auth/v2/login`
- **Метод**: `POST`
- **Требует авторизации**: Нет

**Тело запроса**:
```json
{
  "username": "test_user",
  "password": "password123"
}
```

**Успешный ответ (200 OK)**:
```json
{
  "message": "Login successful",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "812cf290-44e9-4144-85c4-347593fdd75f"
}
```

**Ошибки**:
- `401 Unauthorized` - Неверные учетные данные
- `403 Forbidden` - Аккаунт не верифицирован (в ответе будет передан код верификации)
- `429 Too Many Requests` - Слишком много попыток входа (пользователь временно заблокирован)
- `500 Internal Server Error` - Внутренняя ошибка сервера

### Выход из системы
Выход пользователя из системы и инвалидация токена.

- **URL**: `/api/auth/v2/logout`
- **Метод**: `POST`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ (204 No Content)**:
Ответ без тела.

**Ошибки**:
- `401 Unauthorized` - Токен отсутствует или неверный
- `404 Not Found` - Пользователь не найден
- `500 Internal Server Error` - Внутренняя ошибка сервера

### Обновление токена
Обновление токена доступа с использованием refresh токена.

- **URL**: `/api/auth/v2/refresh`
- **Метод**: `POST`
- **Требует авторизации**: Нет

**Тело запроса**:
```json
{
  "refreshToken": "812cf290-44e9-4144-85c4-347593fdd75f"
}
```

**Успешный ответ (200 OK)**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "Token refreshed successfully"
}
```

**Ошибки**:
- `400 Bad Request` - Отсутствует refresh токен
- `401 Unauthorized` - Недействительный refresh токен
- `500 Internal Server Error` - Внутренняя ошибка сервера

### Изменение пароля
Изменение пароля пользователя.

- **URL**: `/api/auth/v2/change-password`
- **Метод**: `POST`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Тело запроса**:
```json
{
  "currentPassword": "old_password",
  "newPassword": "new_password"
}
```

**Успешный ответ (200 OK)**:
```json
{
  "message": "Password changed successfully. Please login again with your new password."
}
```

**Ошибки**:
- `400 Bad Request` - Неверные учетные данные (текущий пароль)
- `401 Unauthorized` - Пользователь не авторизован
- `500 Internal Server Error` - Внутренняя ошибка сервера

### Валидация токена
Проверка валидности токена доступа.

- **URL**: `/api/auth/v2/validate-token`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ (200 OK)**:
```json
{
  "valid": true,
  "username": "test_user"
}
```

**Ошибки**:
- `401 Unauthorized` - Недействительный токен
- `500 Internal Server Error` - Внутренняя ошибка сервера 