# Clothing Brand - Интернет-магазин одежды с NFT

Проект представляет собой бэкенд для интернет-магазина одежды с интеграцией Telegram, выдачей NFT за покупки и административной панелью управления.

## Архитектура проекта

Проект построен на Spring Boot и использует следующие технологии:
- Spring Boot 3.3
- Spring Security + JWT
- PostgreSQL
- Telegram Bot API
- JPA / Hibernate

## Основные компоненты

### 1. Модуль электронной коммерции
- Управление товарами (CRUD)
- Создание и управление заказами
- Система учета запасов

### 2. Пользовательская система
- Регистрация и аутентификация
- Интеграция с Telegram
- Интеграция с Discord (в разработке)
- Верификация пользователей

### 3. NFT система
- Выдача NFT за покупки
- Система placeholder и reveal NFT после доставки
- Возможность передачи NFT во внешние кошельки

### 4. Telegram интеграция
- Клиентский бот для покупок
- Административный бот для управления заказами

## Схема взаимодействия компонентов

```
┌─────────────────────┐     ┌─────────────────────┐     ┌─────────────────────┐
│                     │     │                     │     │                     │
│  Модуль электронной │     │  Пользовательская   │     │     NFT система     │
│     коммерции       │◄───►│       система       │◄───►│                     │
│                     │     │                     │     │                     │
└─────────┬───────────┘     └─────────┬───────────┘     └─────────┬───────────┘
          │                           │                           │
          │                           │                           │
          │                           ▼                           │
          │             ┌─────────────────────────────┐           │
          └────────────►│                             │◄──────────┘
                        │      Система событий        │
                        │                             │
                        └─────────────┬───────────────┘
                                      │
                                      ▼
                        ┌─────────────────────────────┐
                        │                             │
                        │    Telegram интеграция      │
                        │                             │
                        └─────────────────────────────┘
```

## Архитектура системы аутентификации

Система аутентификации построена на принципах CQRS (Command Query Responsibility Segregation) с использованием паттерна Mediator для обработки команд и запросов.

### Структура системы аутентификации

```
application/auth/
├── core/                        - Основные компоненты и исключения
│   └── exception/               - Базовые исключения авторизации
│
├── cqrs/                        - Реализация CQRS
│   ├── command/                 - Определения команд
│   │   ├── user/                - Команды для пользовательских операций
│   │   └── password/            - Команды для управления паролями
│   │
│   ├── query/                   - Определения запросов
│   │   └── user/                - Запросы о пользователях
│   │
│   ├── handler/                 - Обработчики команд и запросов
│   │   ├── user/                - Обработчики пользовательских команд
│   │   └── password/            - Обработчики команд паролей
│   │
│   └── result/                  - Результаты выполнения команд
│
├── service/                     - Сервисный слой
│   ├── token/                   - Управление токенами
│   ├── notification/            - Уведомления (сброс пароля и др.)
│   ├── security/                - Безопасность (попытки входа и т.д.)
│   └── facade/                  - Сервисы-фасады для доступа к функциональности
│
├── infra/                       - Инфраструктурные компоненты
│   ├── mediator/                - Реализация паттерна Mediator
│   └── repository/              - Репозитории для сброса пароля и др.
│
└── bus/                         - Реализация шин команд и запросов
```

### Поток данных при аутентификации

1. HTTP-запрос поступает в контроллер (`AuthController` или `PasswordResetController`)
2. Контроллер создает команду или запрос и отправляет в сервис-фасад (`AuthServiceCQRS`)
3. Сервис использует Mediator для маршрутизации команды к соответствующему обработчику
4. Обработчик выполняет бизнес-логику и возвращает результат
5. Контроллер формирует HTTP-ответ на основе результата

### Механизмы безопасности

- **Блокировка аккаунта**: После нескольких неудачных попыток входа (`LoginAttemptService`)
- **Верификация**: Двухэтапная верификация через Telegram-бот
- **Управление токенами**: Разделение access и refresh токенов с инвалидацией
- **Сброс пароля**: Безопасный механизм сброса с кодом подтверждения
- **Защита от известных атак**: Обработка ошибок без раскрытия чувствительной информации

## Логика взаимодействия между сервисами

### 1. Покупка товара
1. Пользователь авторизуется через веб-интерфейс или Telegram-бот
2. Создает заказ через OrderService
3. OrderService публикует событие OrderEvent.CREATED
4. OrderEventHandler обрабатывает событие:
   - Отправляет уведомление пользователю через Telegram (если привязан)
   - Отправляет уведомление администраторам через Admin Bot

### 2. Обновление статуса заказа
1. Администратор обновляет статус через API или Admin Bot
2. OrderService публикует соответствующее событие (PAID, SHIPPED, DELIVERED)
3. OrderEventHandler обрабатывает события:
   - При PAID создает NFT через NFTService
   - Отправляет уведомления пользователю о статусе

### 3. Выдача NFT
1. При оплате заказа создается NFT в статусе placeholder
2. NFTService публикует событие NFTEvent.CREATED
3. NFTEventHandler отправляет уведомление пользователю
4. При доставке заказа NFT раскрывается (REVEALED)
5. Пользователь может передать NFT на внешний кошелек

### 4. Интеграция с Telegram
1. Пользователь может привязать Telegram аккаунт через веб-интерфейс:
   - Генерируется код верификации
   - Код вводится в Telegram-бот
   - UserService связывает аккаунты
2. После привязки:
   - Пользователь получает уведомления о заказах
   - Пользователь может просматривать и покупать товары через бот
   - Пользователь получает уведомления о NFT

## Запуск проекта

### С помощью Docker
```bash
docker-compose up -d
```

### Локально
```bash
mvn spring-boot:run
```

## API документация

### Базовая информация
- Базовый URL: `http://localhost:8080/api`
- Формат данных: JSON
- Авторизация: JWT Bearer токен в заголовке `Authorization: Bearer {token}`

### Аутентификация (v2)

#### Регистрация пользователя
- **URL**: `/auth/v2/register`
- **Метод**: `POST`
- **Тело запроса**:
```json
{
  "username": "username",
  "password": "password"
}
```
- **Пример ответа** (200 OK):
```json
{
  "message": "User registered successfully",
  "verificationCode": "AB12CD34"
}
```
- **Пример ответа** (409 Conflict):
```json
{
  "error": "Username already exists",
  "message": "Этот логин уже занят."
}
```

#### Вход пользователя
- **URL**: `/auth/v2/login`
- **Метод**: `POST`
- **Тело запроса**:
```json
{
  "username": "username",
  "password": "password"
}
```
- **Пример ответа** (200 OK):
```json
{
  "message": "Login successful",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```
- **Пример ответа** (403 Forbidden - для неверифицированных аккаунтов):
```json
{
  "message": "Account not verified. Use this code in Telegram bot:",
  "verificationCode": "AB12CD34"
}
```
- **Пример ответа** (429 Too Many Requests - для заблокированных аккаунтов):
```json
{
  "error": "User blocked",
  "message": "Account temporarily blocked due to multiple failed login attempts",
  "minutesLeft": "10"
}
```

#### Обновление токена
- **URL**: `/auth/v2/refresh`
- **Метод**: `POST`
- **Тело запроса**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```
- **Пример ответа** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "message": "Token refreshed successfully"
}
```

#### Выход пользователя
- **URL**: `/auth/v2/logout`
- **Метод**: `POST`
- **Тело запроса**:
```json
{
  "username": "username"
}
```
- **Пример ответа**: 204 No Content

#### Изменение пароля
- **URL**: `/auth/v2/change-password`
- **Метод**: `POST`
- **Тело запроса**:
```json
{
  "username": "username",
  "currentPassword": "currentPassword",
  "newPassword": "newPassword"
}
```
- **Пример ответа** (200 OK):
```json
{
  "message": "Password changed successfully. Please login again with your new password."
}
```
- **Пример ответа** (400 Bad Request):
```json
{
  "error": "Invalid credentials",
  "message": "Current password is incorrect"
}
```

#### Валидация токена
- **URL**: `/auth/v2/validate-token`
- **Метод**: `GET`
- **Заголовки**: `Authorization: Bearer {accessToken}`
- **Пример ответа** (200 OK):
```json
{
  "valid": true,
  "username": "username"
}
```
- **Пример ответа** (401 Unauthorized):
```json
{
  "valid": false,
  "message": "Invalid or expired token"
}
```

### Сброс пароля

#### Инициация сброса пароля
- **URL**: `/auth/v2/password-reset/initiate`
- **Метод**: `POST`
- **Тело запроса**:
```json
{
  "username": "username"
}
```
- **Пример ответа** (200 OK):
```json
{
  "message": "Password reset initiated. Check your notification channel for reset code."
}
```
- **Пример ответа** (404 Not Found):
```json
{
  "error": "UserNotFoundException",
  "message": "User not found: username"
}
```
- **Пример ответа** (409 Conflict):
```json
{
  "error": "PasswordResetException",
  "message": "An active password reset request already exists."
}
```

#### Завершение сброса пароля
- **URL**: `/auth/v2/password-reset/complete`
- **Метод**: `POST`
- **Тело запроса**:
```json
{
  "username": "username",
  "resetCode": "ABC123",
  "newPassword": "newPassword"
}
```
- **Пример ответа** (200 OK):
```json
{
  "message": "Password has been reset successfully. Please login with your new password."
}
```
- **Пример ответа** (401 Unauthorized):
```json
{
  "error": "PasswordResetException",
  "message": "Invalid reset code."
}
```

### Аутентификация (Устаревшая версия)

Для совместимости с существующими клиентами поддерживаются устаревшие эндпоинты:

#### Регистрация пользователя
- **URL**: `/auth/register`
- **Метод**: `POST`
- **Тело запроса**:
```json
{
  "username": "username",
  "password": "password"
}
```
- **Пример ответа** (200 OK):
```json
{
  "message": "User registered successfully",
  "verificationCode": "AB12CD34"
}
```
- **Пример ответа** (409 Conflict):
```json
{
  "error": "Username already exists",
  "message": "Этот логин уже занят."
}
```

#### Вход пользователя
- **URL**: `/auth/login`
- **Метод**: `POST`
- **Тело запроса**:
```json
{
  "username": "username",
  "password": "password"
}
```
- **Пример ответа** (200 OK):
```json
{
  "message": "Login successful",
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```
- **Пример ответа** (403 Forbidden - для неверифицированных аккаунтов):
```json
{
  "message": "Account not verified. Use this code in Telegram bot:",
  "verificationCode": "AB12CD34"
}
```

#### Обновление токена
- **URL**: `/auth/refresh`
- **Метод**: `POST`
- **Тело запроса**:
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```
- **Пример ответа** (200 OK):
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Выход пользователя
- **URL**: `/auth/logout`
- **Метод**: `POST`
- **Тело запроса**:
```json
{
  "username": "username"
}
```
- **Пример ответа**: 204 No Content

### Пользователи

#### Получение данных текущего пользователя
- **URL**: `/users/me`
- **Метод**: `GET`
- **Требуется авторизация**: Да
- **Пример ответа** (200 OK):
```json
{
  "id": 1,
  "username": "username",
  "email": "user@example.com",
  "phoneNumber": "+79123456789",
  "createdAt": "2024-05-20T10:15:30",
  "verified": true
}
```

#### Проверка верификации Telegram-аккаунта
- **URL**: `/users/is-verified`
- **Метод**: `GET`
- **Требуется авторизация**: Да
- **Пример ответа** (200 OK):
```json
{
  "isVerified": true
}
```

#### Обновление профиля пользователя
- **URL**: `/users/update`
- **Метод**: `PUT`
- **Требуется авторизация**: Да
- **Тело запроса**:
```json
{
  "newUsername": "newUsername",
  "email": "new@example.com",
  "phoneNumber": "+79123456789"
}
```
- **Пример ответа** (200 OK):
```json
{
  "message": "User profile updated successfully"
}
```

#### Изменение пароля
- **URL**: `/users/change-password`
- **Метод**: `PUT`
- **Требуется авторизация**: Да
- **Тело запроса**:
```json
{
  "oldPassword": "oldPassword",
  "newPassword": "newPassword"
}
```
- **Пример ответа** (200 OK):
```json
{
  "message": "Password changed successfully"
}
```

#### Получение контактной информации пользователя
- **URL**: `/users/contacts`
- **Метод**: `GET`
- **Требуется авторизация**: Да
- **Пример ответа** (200 OK):
```json
{
  "email": "user@example.com",
  "phoneNumber": "+79123456789"
}
```

### Товары

#### Получение всех товаров
- **URL**: `/products`
- **Метод**: `GET`
- **Пример ответа** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Футболка",
    "description": "Хлопковая футболка",
    "price": 1500,
    "category": "Одежда",
    "sizes": ["S", "M", "L", "XL"],
    "imageUrl": "https://example.com/image.jpg",
    "inStock": true
  },
  {
    "id": 2,
    "name": "Кепка",
    "description": "Кепка с логотипом",
    "price": 1000,
    "category": "Аксессуары",
    "sizes": ["S", "M", "L"],
    "imageUrl": "https://example.com/cap.jpg",
    "inStock": true
  }
]
```

#### Получение товара по ID
- **URL**: `/products/{id}`
- **Метод**: `GET`
- **Пример ответа** (200 OK):
```json
{
  "id": 1,
  "name": "Футболка",
  "description": "Хлопковая футболка",
  "price": 1500,
  "category": "Одежда",
  "sizes": ["S", "M", "L", "XL"],
  "imageUrl": "https://example.com/image.jpg",
  "inStock": true
}
```

#### Получение товаров по размеру
- **URL**: `/products/size/{size}`
- **Метод**: `GET`
- **Пример ответа** (200 OK):
```json
[
  {
    "id": 1,
    "name": "Футболка",
    "description": "Хлопковая футболка",
    "price": 1500,
    "category": "Одежда",
    "sizes": ["S", "M", "L", "XL"],
    "imageUrl": "https://example.com/image.jpg",
    "inStock": true
  }
]
```

### Заказы

#### Создание заказа
- **URL**: `/orders`
- **Метод**: `POST`
- **Требуется авторизация**: Да
- **Тело запроса**:
```json
{
  "productId": 1,
  "quantity": 1,
  "size": "M",
  "email": "user@example.com",
  "fullName": "Иванов Иван Иванович",
  "country": "Россия",
  "address": "г. Москва, ул. Ленина, д. 10, кв. 5",
  "postalCode": "123456",
  "phoneNumber": "+79123456789",
  "telegramUsername": "ivanov_ivan",
  "cryptoAddress": "0x123abc...",
  "orderComment": "Доставить в рабочее время",
  "promoCode": "SALE10",
  "paymentMethod": "CARD"
}
```
- **Пример ответа** (201 Created):
```json
{
  "id": 1,
  "orderNumber": "ORD-2024-123456",
  "status": "CREATED",
  "totalPrice": 1500,
  "createdAt": "2024-05-20T10:15:30",
  "items": [
    {
      "productId": 1,
      "productName": "Футболка",
      "quantity": 1,
      "size": "M",
      "price": 1500
    }
  ],
  "shippingInfo": {
    "fullName": "Иванов Иван Иванович",
    "country": "Россия",
    "address": "г. Москва, ул. Ленина, д. 10, кв. 5",
    "postalCode": "123456",
    "phoneNumber": "+79123456789"
  }
}
```

#### Получение заказа по ID
- **URL**: `/orders/{id}`
- **Метод**: `GET`
- **Требуется авторизация**: Да
- **Пример ответа** (200 OK):
```json
{
  "id": 1,
  "orderNumber": "ORD-2024-123456",
  "status": "PAID",
  "totalPrice": 1500,
  "createdAt": "2024-05-20T10:15:30",
  "items": [
    {
      "productId": 1,
      "productName": "Футболка",
      "quantity": 1,
      "size": "M",
      "price": 1500
    }
  ],
  "shippingInfo": {
    "fullName": "Иванов Иван Иванович",
    "country": "Россия",
    "address": "г. Москва, ул. Ленина, д. 10, кв. 5",
    "postalCode": "123456",
    "phoneNumber": "+79123456789"
  }
}
```

#### Получение всех заказов пользователя
- **URL**: `/orders`
- **Метод**: `GET`
- **Требуется авторизация**: Да
- **Пример ответа** (200 OK):
```json
[
  {
    "id": 1,
    "orderNumber": "ORD-2024-123456",
    "status": "PAID",
    "totalPrice": 1500,
    "createdAt": "2024-05-20T10:15:30",
    "items": [
      {
        "productId": 1,
        "productName": "Футболка",
        "quantity": 1,
        "size": "M",
        "price": 1500
      }
    ]
  },
  {
    "id": 2,
    "orderNumber": "ORD-2024-123457",
    "status": "CREATED",
    "totalPrice": 1000,
    "createdAt": "2024-05-21T11:15:30",
    "items": [
      {
        "productId": 2,
        "productName": "Кепка",
        "quantity": 1,
        "size": "M",
        "price": 1000
      }
    ]
  }
]
```

#### Отмена заказа
- **URL**: `/orders/{id}`
- **Метод**: `DELETE`
- **Требуется авторизация**: Да
- **Пример ответа**: 204 No Content

### NFT

#### Получение NFT текущего пользователя
- **URL**: `/nfts/me`
- **Метод**: `GET`
- **Требуется авторизация**: Да
- **Пример ответа** (200 OK):
```json
[
  {
    "id": 1,
    "placeholderUri": "https://example.com/placeholder.jpg",
    "revealedUri": null,
    "revealed": false,
    "rarity": "RARE",
    "createdAt": "2024-05-20T10:15:30"
  },
  {
    "id": 2,
    "placeholderUri": "https://example.com/placeholder.jpg",
    "revealedUri": "https://example.com/revealed.jpg",
    "revealed": true,
    "rarity": "COMMON",
    "createdAt": "2024-05-19T10:15:30"
  }
]
```

#### Раскрытие NFT (только для администраторов)
- **URL**: `/nfts/{id}/reveal`
- **Метод**: `PUT`
- **Требуется авторизация**: Да (с правами администратора)
- **Тело запроса**:
```json
{
  "revealedUri": "https://example.com/revealed.jpg"
}
```
- **Пример ответа** (200 OK):
```json
{
  "message": "NFT успешно раскрыт"
}
```

### Коды ошибок и статусы ответов

- `200 OK` - Запрос выполнен успешно
- `201 Created` - Ресурс успешно создан
- `204 No Content` - Запрос выполнен успешно, но нет содержимого для ответа
- `400 Bad Request` - Некорректный запрос (неверные параметры)
- `401 Unauthorized` - Отсутствует или недействительный токен авторизации
- `403 Forbidden` - Нет прав доступа к ресурсу
- `404 Not Found` - Ресурс не найден
- `409 Conflict` - Конфликт (например, дублирование имени пользователя)
- `500 Internal Server Error` - Внутренняя ошибка сервера

### Примеры использования API через CURL

#### Регистрация пользователя
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
```

#### Вход пользователя
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"password123"}'
```

#### Получение списка товаров
```bash
curl -X GET http://localhost:8080/api/products
```

#### Создание заказа (с авторизацией)
```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "productId": 1,
    "quantity": 1,
    "size": "M",
    "email": "user@example.com",
    "fullName": "Иванов Иван Иванович",
    "country": "Россия",
    "address": "г. Москва, ул. Ленина, д. 10, кв. 5",
    "postalCode": "123456",
    "phoneNumber": "+79123456789",
    "paymentMethod": "CARD"
  }'
```

#### Получение NFT пользователя
```bash
curl -X GET http://localhost:8080/api/nfts/me \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Примечания
- Для всех эндпоинтов, требующих авторизацию, необходимо передавать токен в заголовке `Authorization: Bearer {token}`
- При создании заказа, после его оплаты автоматически создается NFT в статусе placeholder
- После доставки заказа, NFT может быть раскрыт администратором
- Для работы с Telegram-ботом необходимо верифицировать аккаунт с использованием кода верификации

### Подписки

#### Создание подписки
- **URL**: `/subscriptions`
- **Метод**: `POST`
- **Требуется авторизация**: Да (с правами администратора)
- **Тело запроса**:
```json
{
  "userId": 1,
  "level": "PREMIUM",
  "durationInDays": 30,
  "platform": "WEBSITE"
}
```
- **Примечание**: `level` может принимать значения "BASIC", "STANDARD", "PREMIUM"; `platform` может принимать значения "WEBSITE", "TELEGRAM", "DISCORD", "VK"
- **Пример ответа** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "activationCode": "SUB-12345-ABCDE",
    "subscriptionLevel": "PREMIUM",
    "startDate": "2024-05-20T10:15:30",
    "endDate": "2024-06-19T10:15:30",
    "isActive": false,
    "purchasePlatform": "WEBSITE"
  }
}
```

#### Активация подписки
- **URL**: `/subscriptions/activate`
- **Метод**: `POST`
- **Требуется авторизация**: Да
- **Тело запроса**:
```json
{
  "activationCode": "SUB-12345-ABCDE"
}
```
- **Пример ответа** (200 OK):
```json
{
  "success": true,
  "data": {
    "id": 1,
    "userId": 1,
    "activationCode": "SUB-12345-ABCDE",
    "subscriptionLevel": "PREMIUM",
    "startDate": "2024-05-20T10:15:30",
    "endDate": "2024-06-19T10:15:30",
    "isActive": true,
    "purchasePlatform": "WEBSITE"
  }
}
```

#### Получение подписок пользователя
- **URL**: `/subscriptions/user/{userId}`
- **Метод**: `GET`
- **Требуется авторизация**: Да
- **Пример ответа** (200 OK):
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "userId": 1,
      "activationCode": "SUB-12345-ABCDE",
      "subscriptionLevel": "PREMIUM",
      "startDate": "2024-05-20T10:15:30",
      "endDate": "2024-06-19T10:15:30",
      "isActive": true,
      "purchasePlatform": "WEBSITE"
    },
    {
      "id": 2,
      "userId": 1,
      "activationCode": "SUB-67890-FGHIJ",
      "subscriptionLevel": "BASIC",
      "startDate": "2024-04-20T10:15:30",
      "endDate": "2024-05-20T10:15:30",
      "isActive": false,
      "purchasePlatform": "TELEGRAM"
    }
  ]
}
```

#### Проверка активной подписки
- **URL**: `/subscriptions/check/{userId}/{level}`
- **Метод**: `GET`
- **Требуется авторизация**: Да
- **Пример ответа** (200 OK):
```json
{
  "success": true,
  "data": true
}
```

#### Примеры использования API подписок через CURL

#### Создание подписки (с авторизацией администратора)
```bash
curl -X POST http://localhost:8080/api/subscriptions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "userId": 1,
    "level": "PREMIUM",
    "durationInDays": 30,
    "platform": "WEBSITE"
  }'
```

#### Активация подписки
```bash
curl -X POST http://localhost:8080/api/subscriptions/activate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." \
  -d '{
    "activationCode": "SUB-12345-ABCDE"
  }'
```

#### Проверка активной подписки
```bash
curl -X GET http://localhost:8080/api/subscriptions/check/1/PREMIUM \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## Переменные окружения

- `JWT_SECRET` - секретный ключ для JWT
- `JWT_EXPIRATION` - время жизни токена в миллисекундах
- `JWT_REFRESH_EXPIRATION` - время жизни refresh токена в миллисекундах 