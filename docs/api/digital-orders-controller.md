# Контроллер цифровых заказов

## Общая информация
Контроллер цифровых заказов предоставляет API для создания и управления цифровыми заказами пользователя.

- **Базовый путь**: `/api/digital-orders`
- **Реализация**: `DigitalOrderController.java`

## Эндпоинты

### Создание нового цифрового заказа
Создает новый цифровой заказ для текущего пользователя.

- **URL**: `/api/digital-orders`
- **Метод**: `POST`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Тело запроса**:
```json
{
  "paymentMethod": "CARD",
  "items": [
    {
      "digitalProductId": 1,
      "quantity": 1
    },
    {
      "digitalProductId": 2,
      "quantity": 2
    }
  ]
}
```

**Успешный ответ (201 Created)**:
```json
{
  "id": 1,
  "orderNumber": "ORD-a1b2c3d4",
  "totalPrice": 5000.0,
  "paid": false,
  "paymentMethod": "CARD",
  "createdAt": "2025-05-01T15:30:00",
  "paymentDate": null,
  "items": [
    {
      "id": 1,
      "digitalProductId": 1,
      "productName": "Цифровой дизайн",
      "quantity": 1,
      "price": 2000.0,
      "activationCode": "abc123def456",
      "activationDate": null,
      "expirationDate": null,
      "active": false
    },
    {
      "id": 2,
      "digitalProductId": 2,
      "productName": "Доступ к курсу",
      "quantity": 2,
      "price": 1500.0,
      "activationCode": "ghi789jkl012",
      "activationDate": null,
      "expirationDate": null,
      "active": false
    }
  ]
}
```

**Ошибки**:
- `400 Bad Request` - Некорректные данные заказа
- `401 Unauthorized` - Пользователь не авторизован
- `404 Not Found` - Цифровой продукт не найден

### Получение цифрового заказа по ID
Получает информацию о конкретном цифровом заказе по его идентификатору.

- **URL**: `/api/digital-orders/{id}`
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
  "totalPrice": 5000.0,
  "paid": true,
  "paymentMethod": "CARD",
  "createdAt": "2025-05-01T15:30:00",
  "paymentDate": "2025-05-01T15:45:00",
  "items": [
    {
      "id": 1,
      "digitalProductId": 1,
      "productName": "Цифровой дизайн",
      "quantity": 1,
      "price": 2000.0,
      "activationCode": "abc123def456",
      "activationDate": null,
      "expirationDate": null,
      "active": false
    },
    {
      "id": 2,
      "digitalProductId": 2,
      "productName": "Доступ к курсу",
      "quantity": 2,
      "price": 1500.0,
      "activationCode": "ghi789jkl012",
      "activationDate": null,
      "expirationDate": null,
      "active": false
    }
  ]
}
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован
- `404 Not Found` - Заказ не найден

### Получение всех цифровых заказов пользователя
Возвращает список всех цифровых заказов текущего пользователя.

- **URL**: `/api/digital-orders`
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
    "totalPrice": 5000.0,
    "paid": true,
    "paymentMethod": "CARD",
    "createdAt": "2025-05-01T15:30:00",
    "paymentDate": "2025-05-01T15:45:00",
    "items": [
      {
        "id": 1,
        "digitalProductId": 1,
        "productName": "Цифровой дизайн",
        "quantity": 1,
        "price": 2000.0,
        "activationCode": "abc123def456",
        "activationDate": null,
        "expirationDate": null,
        "active": false
      },
      {
        "id": 2,
        "digitalProductId": 2,
        "productName": "Доступ к курсу",
        "quantity": 2,
        "price": 1500.0,
        "activationCode": "ghi789jkl012",
        "activationDate": null,
        "expirationDate": null,
        "active": false
      }
    ]
  }
]
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован

### Получение статуса цифрового заказа
Возвращает информацию о статусе цифрового заказа по его идентификатору.

- **URL**: `/api/digital-orders/{id}/status`
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
  "isPaid": true,
  "paymentMethod": "CARD",
  "createdAt": "2025-05-01T15:30:00",
  "paymentDate": "2025-05-01T15:45:00"
}
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован
- `404 Not Found` - Заказ не найден

### Активация цифрового продукта
Активирует цифровой продукт по его ID в заказе.

- **URL**: `/api/digital-orders/items/{itemId}/activate`
- **Метод**: `POST`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Параметры пути**:
- `itemId` - Идентификатор позиции заказа

**Успешный ответ (200 OK)**:
```json
{
  "id": 1,
  "digitalProductId": 1,
  "productName": "Цифровой дизайн",
  "quantity": 1,
  "price": 2000.0,
  "activationCode": "abc123def456",
  "activationDate": "2025-05-02T10:15:00",
  "expirationDate": "2025-08-02T10:15:00",
  "active": true
}
```

**Ошибки**:
- `400 Bad Request` - Заказ не оплачен или другая ошибка при активации
- `401 Unauthorized` - Пользователь не авторизован
- `403 Forbidden` - Пользователь не имеет прав на активацию данного продукта
- `404 Not Found` - Позиция заказа не найдена 