# API Documentation

## Оглавление
- [Аутентификация](#аутентификация)
  - [Регистрация](#регистрация)
  - [Авторизация](#авторизация)
  - [Выход из системы](#выход-из-системы)
  - [Обновление токена](#обновление-токена)
  - [Изменение пароля](#изменение-пароля)
  - [Валидация токена](#валидация-токена)
- [Пользователи](#пользователи)
  - [Получение информации о текущем пользователе](#получение-информации-о-текущем-пользователе)
  - [Проверка верификации в Telegram](#проверка-верификации-в-telegram)
  - [Обновление профиля](#обновление-профиля)
  - [Получение контактной информации](#получение-контактной-информации)
- [Заказы](#заказы)
  - [Создание заказа](#создание-заказа)
  - [Получение заказа по ID](#получение-заказа-по-id)
  - [Получение всех заказов пользователя](#получение-всех-заказов-пользователя)
  - [Отмена заказа](#отмена-заказа)
- [Товары](#товары)
  - [Получение всех товаров](#получение-всех-товаров)
  - [Получение товара по ID](#получение-товара-по-id)
  - [Получение товаров по размеру](#получение-товаров-по-размеру)

## Аутентификация

### Регистрация
Регистрация нового пользователя в системе.

**URL**: `/api/auth/v2/register`  
**Метод**: `POST`  
**Требует авторизации**: Нет

**Тело запроса**:
```json
{
  "username": "test_user",
  "password": "password123"
}
```

**Успешный ответ**:
```json
{
  "message": "User registered successfully",
  "verificationCode": "a1b2c3d4"
}
```

**Коды ответов**:
- `200 OK` - Успешная регистрация
- `409 Conflict` - Пользователь с таким именем уже существует
- `500 Internal Server Error` - Внутренняя ошибка сервера

### Авторизация
Вход пользователя в систему и получение токена доступа.

**URL**: `/api/auth/v2/login`  
**Метод**: `POST`  
**Требует авторизации**: Нет

**Тело запроса**:
```json
{
  "username": "test_user",
  "password": "password123"
}
```

**Успешный ответ**:
```json
{
  "message": "Login successful",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "812cf290-44e9-4144-85c4-347593fdd75f"
}
```

**Коды ответов**:
- `200 OK` - Успешная авторизация
- `401 Unauthorized` - Неверные учетные данные
- `403 Forbidden` - Требуется верификация
- `429 Too Many Requests` - Слишком много попыток входа (пользователь временно заблокирован)
- `500 Internal Server Error` - Внутренняя ошибка сервера

### Выход из системы
Выход пользователя из системы и инвалидация токена.

**URL**: `/api/auth/v2/logout`  
**Метод**: `POST`  
**Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ**:
- `204 No Content` - Успешный выход

**Коды ответов**:
- `204 No Content` - Успешный выход
- `401 Unauthorized` - Токен отсутствует или неверный
- `404 Not Found` - Пользователь не найден
- `500 Internal Server Error` - Внутренняя ошибка сервера

### Обновление токена
Обновление токена доступа с использованием refresh токена.

**URL**: `/api/auth/v2/refresh`  
**Метод**: `POST`  
**Требует авторизации**: Нет

**Тело запроса**:
```json
{
  "refreshToken": "812cf290-44e9-4144-85c4-347593fdd75f"
}
```

**Успешный ответ**:
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "message": "Token refreshed successfully"
}
```

**Коды ответов**:
- `200 OK` - Токен успешно обновлен
- `400 Bad Request` - Отсутствует refresh токен
- `401 Unauthorized` - Недействительный refresh токен
- `500 Internal Server Error` - Внутренняя ошибка сервера

### Изменение пароля
Изменение пароля пользователя.

**URL**: `/api/auth/v2/change-password`  
**Метод**: `POST`  
**Требует авторизации**: Да (Bearer Token)

**Тело запроса**:
```json
{
  "username": "test_user",
  "currentPassword": "old_password",
  "newPassword": "new_password"
}
```

**Успешный ответ**:
```json
{
  "message": "Password changed successfully. Please login again with your new password."
}
```

**Коды ответов**:
- `200 OK` - Пароль успешно изменен
- `400 Bad Request` - Неверные учетные данные (текущий пароль)
- `500 Internal Server Error` - Внутренняя ошибка сервера

### Валидация токена
Проверка валидности токена доступа.

**URL**: `/api/auth/v2/validate-token`  
**Метод**: `GET`  
**Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ**:
```json
{
  "valid": true,
  "username": "test_user"
}
```

**Коды ответов**:
- `200 OK` - Токен валиден
- `401 Unauthorized` - Недействительный токен
- `500 Internal Server Error` - Внутренняя ошибка сервера

## Пользователи

### Получение информации о текущем пользователе
Получение информации о текущем аутентифицированном пользователе.

**URL**: `/api/users/me`  
**Метод**: `GET`  
**Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ**:
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

**Коды ответов**:
- `200 OK` - Успешное получение данных
- `401 Unauthorized` - Пользователь не авторизован
- `404 Not Found` - Пользователь не найден

### Проверка верификации в Telegram
Проверка статуса верификации пользователя в Telegram.

**URL**: `/api/users/is-verified`  
**Метод**: `GET`  
**Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ**:
```json
{
  "isVerified": true
}
```

**Коды ответов**:
- `200 OK` - Успешное получение статуса
- `401 Unauthorized` - Пользователь не авторизован

### Обновление профиля
Обновление профиля пользователя.

**URL**: `/api/users/update`  
**Метод**: `PUT`  
**Требует авторизации**: Да (Bearer Token)

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

**Успешный ответ**:
```json
{
  "message": "User profile updated successfully"
}
```

**Коды ответов**:
- `200 OK` - Профиль успешно обновлен
- `400 Bad Request` - Не предоставлены данные для обновления
- `401 Unauthorized` - Пользователь не авторизован

### Получение контактной информации
Получение контактной информации пользователя.

**URL**: `/api/users/contacts`  
**Метод**: `GET`  
**Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ**:
```json
{
  "email": "user@example.com",
  "phoneNumber": "+79123456789"
}
```

**Коды ответов**:
- `200 OK` - Успешное получение контактов
- `401 Unauthorized` - Пользователь не авторизован

## Заказы

### Создание заказа
Создание нового заказа.

**URL**: `/api/orders`  
**Метод**: `POST`  
**Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Тело запроса**:
```json
{
  "productId": 1,
  "quantity": 1,
  "size": "M",
  "email": "user@example.com",
  "fullName": "Иван Иванов",
  "country": "Россия",
  "address": "ул. Примерная, д. 123, кв. 45",
  "postalCode": "123456",
  "phoneNumber": "+79123456789",
  "telegramUsername": "telegram_user",
  "cryptoAddress": "0x123...",
  "orderComment": "Доставить до двери",
  "promoCode": "PROMO10",
  "paymentMethod": "CARD"
}
```

**Успешный ответ**:
```json
{
  "id": 1,
  "orderNumber": "ORD-a1b2c3d4",
  "productName": "Футболка",
  "size": "M",
  "quantity": 1,
  "price": 1500.0,
  "telegramUsername": "telegram_user",
  "paymentMethod": "CARD",
  "orderComment": "Доставить до двери",
  "createdAt": "2025-05-01T15:30:00",
  "status": "NEW"
}
```

**Коды ответов**:
- `201 Created` - Заказ успешно создан
- `400 Bad Request` - Некорректные данные заказа
- `401 Unauthorized` - Пользователь не авторизован
- `404 Not Found` - Товар не найден

### Получение заказа по ID
Получение информации о заказе по его идентификатору.

**URL**: `/api/orders/{id}`  
**Метод**: `GET`  
**Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ**:
```json
{
  "id": 1,
  "orderNumber": "ORD-a1b2c3d4",
  "productName": "Футболка",
  "size": "M",
  "quantity": 1,
  "price": 1500.0,
  "telegramUsername": "telegram_user",
  "paymentMethod": "CARD",
  "orderComment": "Доставить до двери",
  "createdAt": "2025-05-01T15:30:00",
  "status": "NEW"
}
```

**Коды ответов**:
- `200 OK` - Заказ найден
- `401 Unauthorized` - Пользователь не авторизован
- `404 Not Found` - Заказ не найден

### Получение всех заказов пользователя
Получение списка всех заказов текущего пользователя.

**URL**: `/api/orders`  
**Метод**: `GET`  
**Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ**:
```json
[
  {
    "id": 1,
    "orderNumber": "ORD-a1b2c3d4",
    "productName": "Футболка",
    "size": "M",
    "quantity": 1,
    "price": 1500.0,
    "telegramUsername": "telegram_user",
    "paymentMethod": "CARD",
    "orderComment": "Доставить до двери",
    "createdAt": "2025-05-01T15:30:00",
    "status": "NEW"
  },
  {
    "id": 2,
    "orderNumber": "ORD-e5f6g7h8",
    "productName": "Худи",
    "size": "L",
    "quantity": 1,
    "price": 3000.0,
    "telegramUsername": "telegram_user",
    "paymentMethod": "CRYPTO",
    "orderComment": "",
    "createdAt": "2025-05-02T10:15:00",
    "status": "PROCESSING"
  }
]
```

**Коды ответов**:
- `200 OK` - Список заказов успешно получен
- `401 Unauthorized` - Пользователь не авторизован

### Отмена заказа
Отмена заказа пользователя.

**URL**: `/api/orders/{id}`  
**Метод**: `DELETE`  
**Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ**:
- `204 No Content` - Заказ успешно отменен

**Коды ответов**:
- `204 No Content` - Заказ успешно отменен
- `401 Unauthorized` - Пользователь не авторизован
- `403 Forbidden` - Доступ запрещен (не владелец заказа)
- `404 Not Found` - Заказ не найден

## Товары

### Получение всех товаров
Получение списка всех доступных товаров.

**URL**: `/api/products`  
**Метод**: `GET`  
**Требует авторизации**: Нет

**Успешный ответ**:
```json
[
  {
    "id": 1,
    "name": "Футболка",
    "description": "Хлопковая футболка с логотипом",
    "price": 1500.0,
    "imageUrl": "https://example.com/tshirt.jpg",
    "availableQuantityS": 10,
    "availableQuantityM": 15,
    "availableQuantityL": 8,
    "createdAt": "2025-04-01T12:00:00",
    "updatedAt": "2025-04-15T14:30:00"
  },
  {
    "id": 2,
    "name": "Худи",
    "description": "Теплое худи с капюшоном",
    "price": 3000.0,
    "imageUrl": "https://example.com/hoodie.jpg",
    "availableQuantityS": 5,
    "availableQuantityM": 8,
    "availableQuantityL": 10,
    "createdAt": "2025-04-02T14:00:00",
    "updatedAt": "2025-04-16T10:15:00"
  }
]
```

**Коды ответов**:
- `200 OK` - Список товаров успешно получен

### Получение товара по ID
Получение информации о товаре по его идентификатору.

**URL**: `/api/products/{id}`  
**Метод**: `GET`  
**Требует авторизации**: Нет

**Успешный ответ**:
```json
{
  "id": 1,
  "name": "Футболка",
  "description": "Хлопковая футболка с логотипом",
  "price": 1500.0,
  "imageUrl": "https://example.com/tshirt.jpg",
  "availableQuantityS": 10,
  "availableQuantityM": 15,
  "availableQuantityL": 8,
  "createdAt": "2025-04-01T12:00:00",
  "updatedAt": "2025-04-15T14:30:00"
}
```

**Коды ответов**:
- `200 OK` - Товар найден
- `404 Not Found` - Товар не найден

### Получение товаров по размеру
Получение списка товаров по указанному размеру.

**URL**: `/api/products/size/{size}`  
**Метод**: `GET`  
**Требует авторизации**: Нет

**Параметры URL**:
- `{size}` - Размер товара (S, M, L, XL)

**Успешный ответ**:
```json
[
  {
    "id": 1,
    "name": "Футболка",
    "description": "Хлопковая футболка с логотипом",
    "price": 1500.0,
    "imageUrl": "https://example.com/tshirt.jpg",
    "availableQuantityS": 10,
    "availableQuantityM": 15,
    "availableQuantityL": 8,
    "createdAt": "2025-04-01T12:00:00",
    "updatedAt": "2025-04-15T14:30:00"
  },
  {
    "id": 3,
    "name": "Свитшот",
    "description": "Стильный свитшот",
    "price": 2500.0,
    "imageUrl": "https://example.com/sweatshirt.jpg",
    "availableQuantityS": 12,
    "availableQuantityM": 6,
    "availableQuantityL": 3,
    "createdAt": "2025-04-03T09:00:00",
    "updatedAt": "2025-04-17T16:45:00"
  }
]
```

**Коды ответов**:
- `200 OK` - Список товаров по размеру успешно получен 