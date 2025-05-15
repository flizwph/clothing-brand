# Контроллер заказов

## Общая информация
Контроллер заказов предоставляет API для создания и управления заказами пользователя.

- **Базовый путь**: `/api/orders`
- **Реализация**: `OrderController.java`

## Эндпоинты

### Создание нового заказа
Создает новый заказ для текущего пользователя.

- **URL**: `/api/orders`
- **Метод**: `POST`
- **Требует авторизации**: Да (Bearer Token)

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

**Успешный ответ (201 Created)**:
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

**Ошибки**:
- `400 Bad Request` - Некорректные данные заказа
- `401 Unauthorized` - Пользователь не авторизован
- `404 Not Found` - Товар не найден

### Получение заказа по ID
Получает информацию о конкретном заказе по его идентификатору.

- **URL**: `/api/orders/{id}`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Параметры пути**:
- `id` - Идентификатор заказа

**Успешный ответ (200 OK)**:
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

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован
- `404 Not Found` - Заказ не найден

### Получение всех заказов пользователя
Возвращает список всех заказов текущего пользователя.

- **URL**: `/api/orders`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ (200 OK)**:
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

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован

### Получение статуса заказа
Возвращает информацию о статусе заказа по его идентификатору.

- **URL**: `/api/orders/{id}/status`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Параметры пути**:
- `id` - Идентификатор заказа

**Успешный ответ (200 OK)**:
```json
{
  "orderNumber": "ORD-a1b2c3d4",
  "status": "PROCESSING",
  "createdAt": "2025-05-01T15:30:00"
}
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован
- `404 Not Found` - Заказ не найден

### Отмена заказа
Отменяет заказ пользователя.

- **URL**: `/api/orders/{id}`
- **Метод**: `DELETE`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Параметры пути**:
- `id` - Идентификатор заказа

**Успешный ответ (204 No Content)**:
Ответ без тела.

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован
- `403 Forbidden` - Доступ запрещен (не владелец заказа)
- `404 Not Found` - Заказ не найден 