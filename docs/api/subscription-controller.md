# Контроллеры подписок

## Общая информация
Контроллеры подписок предоставляют API для создания, активации и проверки подписок пользователей.

### Контроллер подписок
- **Базовый путь**: `/api/subscriptions`
- **Реализация**: `SubscriptionController.java`

### Контроллер десктопных подписок
- **Базовый путь**: `/api/desktop`
- **Реализация**: `DesktopSubscriptionController.java`

## Эндпоинты

### Создание подписки
Создает новую подписку для пользователя.

- **URL**: `/api/subscriptions`
- **Метод**: `POST`
- **Требует авторизации**: Да (Bearer Token, ADMIN роль)

**Тело запроса**:
```json
{
  "userId": 1,
  "level": "STANDARD",
  "durationInDays": 30,
  "platform": "WEBSITE"
}
```

**Примечание**:
Поле `level` может принимать одно из следующих значений: `BASIC`, `STANDARD`, `PREMIUM`.
Поле `platform` может принимать одно из следующих значений: `WEBSITE`, `TELEGRAM`, `DISCORD`, `DESKTOP`.

**Успешный ответ (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "activationCode": "SUB-a1b2c3d4",
    "subscriptionLevel": "STANDARD",
    "startDate": "2025-05-01T15:30:00",
    "endDate": "2025-05-31T15:30:00",
    "isActive": true,
    "purchasePlatform": "WEBSITE"
  }
}
```

**Ошибки**:
- `400 Bad Request` - Некорректные данные подписки
- `401 Unauthorized` - Пользователь не авторизован
- `403 Forbidden` - Недостаточно прав для создания подписки
- `404 Not Found` - Пользователь не найден

### Активация подписки
Активирует подписку по коду активации.

- **URL**: `/api/subscriptions/activate`
- **Метод**: `POST`
- **Требует авторизации**: Да (Bearer Token)

**Тело запроса**:
```json
{
  "activationCode": "SUB-a1b2c3d4"
}
```

**Успешный ответ (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "activationCode": "SUB-a1b2c3d4",
    "subscriptionLevel": "STANDARD",
    "startDate": "2025-05-01T15:30:00",
    "endDate": "2025-05-31T15:30:00",
    "isActive": true,
    "purchasePlatform": "WEBSITE"
  }
}
```

**Ошибки**:
- `400 Bad Request` - Некорректный код активации
- `401 Unauthorized` - Пользователь не авторизован
- `404 Not Found` - Подписка не найдена

### Получение подписок пользователя
Возвращает список активных подписок пользователя.

- **URL**: `/api/subscriptions/user/{userId}`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token, пользователь или админ)

**Параметры пути**:
- `userId` - Идентификатор пользователя

**Успешный ответ (200 OK)**:
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 1,
      "activationCode": "SUB-a1b2c3d4",
      "subscriptionLevel": "STANDARD",
      "startDate": "2025-05-01T15:30:00",
      "endDate": "2025-05-31T15:30:00",
      "isActive": true,
      "purchasePlatform": "WEBSITE"
    },
    {
      "id": 2,
      "userId": 1,
      "activationCode": "SUB-e5f6g7h8",
      "subscriptionLevel": "PREMIUM",
      "startDate": "2025-05-10T12:00:00",
      "endDate": "2025-06-10T12:00:00",
      "isActive": true,
      "purchasePlatform": "TELEGRAM"
    }
  ]
}
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован
- `403 Forbidden` - Недостаточно прав для просмотра подписок
- `404 Not Found` - Пользователь не найден

### Проверка активности подписки
Проверяет, активна ли у пользователя подписка определенного уровня.

- **URL**: `/api/subscriptions/check/{userId}/{level}`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Параметры пути**:
- `userId` - Идентификатор пользователя
- `level` - Уровень подписки (BASIC, STANDARD, PREMIUM)

**Успешный ответ (200 OK)**:
```json
{
  "success": true,
  "data": true
}
```

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован
- `404 Not Found` - Пользователь не найден

## Десктопный контроллер подписок

### Активация подписки для десктопного приложения
Активирует подписку по коду активации для десктопного приложения.

- **URL**: `/api/desktop/activate`
- **Метод**: `POST`
- **Требует авторизации**: Нет

**Тело запроса**:
```json
{
  "activationCode": "SUB-a1b2c3d4"
}
```

**Успешный ответ (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "activationCode": "SUB-a1b2c3d4",
    "subscriptionLevel": "STANDARD",
    "startDate": "2025-05-01T15:30:00",
    "endDate": "2025-05-31T15:30:00",
    "isActive": true,
    "purchasePlatform": "DESKTOP"
  }
}
```

**Ошибки**:
- `400 Bad Request` - Некорректный код активации
- `404 Not Found` - Подписка не найдена

### Проверка статуса подписки по коду активации
Проверяет статус подписки по коду активации для десктопного приложения.

- **URL**: `/api/desktop/check/{activationCode}`
- **Метод**: `GET`
- **Требует авторизации**: Нет

**Параметры пути**:
- `activationCode` - Код активации подписки

**Успешный ответ (200 OK)**:
```json
{
  "success": true,
  "data": {
    "isActive": true,
    "level": "STANDARD",
    "expirationDate": "2025-05-31T15:30:00"
  }
}
```

**Ответ для неактивной подписки (200 OK)**:
```json
{
  "success": true,
  "data": {
    "isActive": false,
    "level": "STANDARD",
    "expirationDate": "2025-01-31T15:30:00"
  }
}
```

**Ошибки**:
- `404 Not Found` - Подписка не найдена

### Статус подписки для авторизованного пользователя
Возвращает подробную информацию о статусе подписки для авторизованного пользователя.

- **URL**: `/api/desktop/status`
- **Метод**: `GET`
- **Требует авторизации**: Да (Bearer Token)

**Успешный ответ (200 OK)**:
```json
{
  "success": true,
  "data": {
    "status": "ACTIVE",
    "level": "PREMIUM",
    "activationDate": "2023-05-01T15:30:00",
    "expirationDate": "2024-05-01T15:30:00"
  }
}
```

**Возможные значения статуса**:
- `ACTIVE` - Подписка активна
- `EXPIRED` - Срок действия подписки истек
- `INACTIVE` - Подписка неактивна (не найдена)
- `PENDING` - Подписка ожидает активации

**Ошибки**:
- `401 Unauthorized` - Пользователь не авторизован 