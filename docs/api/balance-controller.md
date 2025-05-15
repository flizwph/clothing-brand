# Контроллер баланса

## Общая информация
Контроллер баланса предоставляет API для управления финансовым балансом пользователя и просмотра истории транзакций.

- **Базовый путь**: `/api/balance`
- **Реализация**: `BalanceController.java`

## Эндпоинты

### Получение текущего баланса
Возвращает текущий баланс пользователя.

- **URL**: `/api/balance`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ (200 OK)**:
```json
{
  "balance": 5000.00,
  "username": "test_user"
}
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован

### Создание запроса на пополнение баланса
Создает запрос на пополнение баланса пользователя.

- **URL**: `/api/balance/deposit`
- **Метод**: `POST`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Тело запроса**:
```json
{
  "amount": 1000.00
}
```

**Успешный ответ (201 Created)**:
```json
{
  "transactionCode": "DEP-a1b2c3d4",
  "amount": 1000.00,
  "paymentInstructions": "Переведите указанную сумму на счет XXX с указанием кода транзакции",
  "qrCodeUrl": "https://example.com/qr/DEP-a1b2c3d4",
  "message": "Запрос на пополнение создан"
}
```

**Ошибки**:
- `400 Bad Request` - Некорректная сумма
- `401 Unauthorized` - Пользователь не авторизован

### Получение истории транзакций с пагинацией
Возвращает историю транзакций пользователя с пагинацией.

- **URL**: `/api/balance/transactions`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Параметры запроса**:
- `page` - Номер страницы (по умолчанию 0)
- `size` - Размер страницы (по умолчанию 20)
- `sort` - Поле для сортировки (по умолчанию createdAt,desc)

**Успешный ответ (200 OK)**:
```json
{
  "content": [
    {
      "id": 1,
      "transactionCode": "DEP-a1b2c3d4",
      "type": "DEPOSIT",
      "amount": 1000.00,
      "description": "Пополнение баланса",
      "status": "COMPLETED",
      "createdAt": "2025-05-01T15:30:00"
    },
    {
      "id": 2,
      "transactionCode": "ORD-e5f6g7h8",
      "type": "ORDER_PAYMENT",
      "amount": 1500.00,
      "description": "Оплата заказа #12345",
      "status": "COMPLETED",
      "createdAt": "2025-05-02T10:15:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": [
      "createdAt: DESC"
    ]
  },
  "totalElements": 2,
  "totalPages": 1
}
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован

### Получение полной истории транзакций
Возвращает полную историю транзакций пользователя без пагинации.

- **URL**: `/api/balance/transactions/all`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Успешный ответ (200 OK)**:
```json
[
  {
    "id": 1,
    "transactionCode": "DEP-a1b2c3d4",
    "type": "DEPOSIT",
    "amount": 1000.00,
    "description": "Пополнение баланса",
    "status": "COMPLETED",
    "createdAt": "2025-05-01T15:30:00"
  },
  {
    "id": 2,
    "transactionCode": "ORD-e5f6g7h8",
    "type": "ORDER_PAYMENT",
    "amount": 1500.00,
    "description": "Оплата заказа #12345",
    "status": "COMPLETED",
    "createdAt": "2025-05-02T10:15:00"
  }
]
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован

### Получение отфильтрованной истории транзакций
Возвращает отфильтрованную историю транзакций пользователя.

- **URL**: `/api/balance/transactions/filtered`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Параметры запроса**:
- `type` - Тип транзакции (DEPOSIT, WITHDRAWAL, ORDER_PAYMENT)
- `status` - Статус транзакции (PENDING, COMPLETED, CANCELLED, FAILED)

**Успешный ответ (200 OK)**:
```json
[
  {
    "id": 1,
    "transactionCode": "DEP-a1b2c3d4",
    "type": "DEPOSIT",
    "amount": 1000.00,
    "description": "Пополнение баланса",
    "status": "COMPLETED",
    "createdAt": "2025-05-01T15:30:00"
  }
]
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован

### Получение детальной информации о транзакции
Возвращает детальную информацию о конкретной транзакции.

- **URL**: `/api/balance/transactions/{id}`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Параметры пути**:
- `id` - Идентификатор транзакции

**Успешный ответ (200 OK)**:
```json
{
  "id": 1,
  "transactionCode": "DEP-a1b2c3d4",
  "type": "DEPOSIT",
  "amount": 1000.00,
  "description": "Пополнение баланса",
  "status": "COMPLETED",
  "createdAt": "2025-05-01T15:30:00",
  "completedAt": "2025-05-01T15:35:00",
  "paymentDetails": {
    "method": "BANK_TRANSFER",
    "paymentId": "7890123",
    "payerInfo": "User Bank Account"
  }
}
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован
- `403 Forbidden` - Доступ запрещен (чужая транзакция)
- `404 Not Found` - Транзакция не найдена

### Отмена транзакции
Отменяет незавершенную транзакцию пополнения баланса.

- **URL**: `/api/balance/transactions/{id}/cancel`
- **Метод**: `POST`
- **Требует авторизации**: Да (Bearer Token)

**Заголовки**:
- `Authorization: Bearer {access_token}`

**Параметры пути**:
- `id` - Идентификатор транзакции

**Успешный ответ (200 OK)**:
```json
{
  "id": 1,
  "transactionCode": "DEP-a1b2c3d4",
  "type": "DEPOSIT",
  "amount": 1000.00,
  "description": "Пополнение баланса",
  "status": "CANCELLED",
  "createdAt": "2025-05-01T15:30:00",
  "completedAt": null
}
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован
- `403 Forbidden` - Доступ запрещен (чужая транзакция)
- `404 Not Found` - Транзакция не найдена
- `400 Bad Request` - Невозможно отменить завершенную транзакцию 