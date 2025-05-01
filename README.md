# Clothing Brand API

Документация REST API для интернет-магазина одежды Clothing Brand.

## Содержание

- [Аутентификация](#аутентификация)
- [Пользователи](#пользователи)
- [Товары](#товары)
- [Заказы](#заказы)
- [Платежи и баланс](#платежи-и-баланс)
- [Промокоды](#промокоды)
- [NFT](#nft)

## Аутентификация

### Регистрация пользователя

**Запрос:**
```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "user123",
  "email": "user@example.com",
  "password": "securepassword",
  "phoneNumber": "+79991234567"
}
```

**Ответ (200 OK):**
```json
{
  "id": 1,
  "username": "user123",
  "email": "user@example.com",
  "role": "customer",
  "verified": false,
  "createdAt": "2025-05-01T10:00:00"
}
```

### Авторизация

**Запрос:**
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "user123",
  "password": "securepassword"
}
```

**Ответ (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### Обновление токена

**Запрос:**
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Ответ (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### Выход из системы

**Запрос:**
```http
POST /api/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ (200 OK):**
```json
{
  "message": "Выход из системы выполнен успешно"
}
```

## Пользователи

### Получение данных текущего пользователя

**Запрос:**
```http
GET /api/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ (200 OK):**
```json
{
  "id": 1,
  "username": "user123",
  "email": "user@example.com",
  "role": "customer",
  "verified": true,
  "balance": 1000.00,
  "telegramUsername": "user123",
  "discordUsername": "user123#1234",
  "createdAt": "2025-05-01T10:00:00",
  "lastLogin": "2025-05-01T15:00:00"
}
```

### Обновление профиля пользователя

**Запрос:**
```http
PUT /api/users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "email": "newemail@example.com",
  "phoneNumber": "+79991234568"
}
```

**Ответ (200 OK):**
```json
{
  "id": 1,
  "username": "user123",
  "email": "newemail@example.com",
  "phoneNumber": "+79991234568",
  "message": "Профиль успешно обновлен"
}
```

### Смена пароля

**Запрос:**
```http
PUT /api/users/me/password
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "currentPassword": "securepassword",
  "newPassword": "newSecurePassword"
}
```

**Ответ (200 OK):**
```json
{
  "message": "Пароль успешно изменен"
}
```

### Привязка Telegram аккаунта

**Запрос:**
```http
POST /api/users/me/telegram/link
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "telegramId": "825885701",
  "telegramUsername": "user123",
  "verificationCode": "ABC123"
}
```

**Ответ (200 OK):**
```json
{
  "message": "Telegram аккаунт успешно привязан"
}
```

## Товары

### Получение списка товаров

**Запрос:**
```http
GET /api/products?page=0&size=10&sort=createdAt,desc
```

**Ответ (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Футболка с логотипом",
      "description": "Хлопковая футболка с логотипом бренда",
      "price": 1500.00,
      "categories": ["одежда", "футболки"],
      "sizes": ["S", "M", "L", "XL"],
      "colors": ["черный", "белый", "красный"],
      "stockS": 10,
      "stockM": 15,
      "stockL": 20,
      "stockXL": 5,
      "images": ["image1.jpg", "image2.jpg"],
      "createdAt": "2025-05-01T10:00:00"
    },
    {
      "id": 2,
      "name": "Толстовка базовая",
      "description": "Базовая толстовка из органического хлопка",
      "price": 3500.00,
      "categories": ["одежда", "толстовки"],
      "sizes": ["S", "M", "L", "XL"],
      "colors": ["черный", "серый"],
      "stockS": 5,
      "stockM": 10,
      "stockL": 10,
      "stockXL": 5,
      "images": ["image3.jpg", "image4.jpg"],
      "createdAt": "2025-05-01T11:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 120,
  "totalPages": 12,
  "last": false,
  "first": true,
  "empty": false
}
```

### Получение товара по ID

**Запрос:**
```http
GET /api/products/1
```

**Ответ (200 OK):**
```json
{
  "id": 1,
  "name": "Футболка с логотипом",
  "description": "Хлопковая футболка с логотипом бренда",
  "price": 1500.00,
  "categories": ["одежда", "футболки"],
  "sizes": ["S", "M", "L", "XL"],
  "colors": ["черный", "белый", "красный"],
  "stockS": 10,
  "stockM": 15,
  "stockL": 20,
  "stockXL": 5,
  "images": ["image1.jpg", "image2.jpg"],
  "createdAt": "2025-05-01T10:00:00",
  "updatedAt": "2025-05-01T12:00:00"
}
```

### Поиск товаров

**Запрос:**
```http
GET /api/products/search?query=футболка&category=одежда&minPrice=1000&maxPrice=2000&page=0&size=10
```

**Ответ (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "name": "Футболка с логотипом",
      "description": "Хлопковая футболка с логотипом бренда",
      "price": 1500.00,
      "categories": ["одежда", "футболки"],
      "sizes": ["S", "M", "L", "XL"],
      "colors": ["черный", "белый", "красный"],
      "stockS": 10,
      "stockM": 15,
      "stockL": 20,
      "stockXL": 5,
      "images": ["image1.jpg", "image2.jpg"],
      "createdAt": "2025-05-01T10:00:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 5,
  "totalPages": 1,
  "last": true,
  "first": true,
  "empty": false
}
```

## Заказы

### Создание заказа

**Запрос:**
```http
POST /api/orders
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "items": [
    {
      "productId": 1,
      "size": "M",
      "color": "черный",
      "quantity": 2
    },
    {
      "productId": 2,
      "size": "L",
      "color": "серый",
      "quantity": 1
    }
  ],
  "deliveryAddress": {
    "country": "Россия",
    "city": "Москва",
    "street": "Ленина",
    "house": "10",
    "apartment": "42",
    "postalCode": "123456"
  },
  "paymentMethod": "BALANCE",
  "promoCode": "SUMMER2025"
}
```

**Ответ (201 Created):**
```json
{
  "id": 1,
  "orderNumber": "ORD-12345678",
  "status": "NEW",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "Футболка с логотипом",
      "size": "M",
      "color": "черный",
      "quantity": 2,
      "price": 1500.00,
      "totalPrice": 3000.00
    },
    {
      "id": 2,
      "productId": 2,
      "productName": "Толстовка базовая",
      "size": "L",
      "color": "серый",
      "quantity": 1,
      "price": 3500.00,
      "totalPrice": 3500.00
    }
  ],
  "subtotal": 6500.00,
  "discount": 650.00,
  "deliveryPrice": 500.00,
  "total": 6350.00,
  "deliveryAddress": {
    "country": "Россия",
    "city": "Москва",
    "street": "Ленина",
    "house": "10",
    "apartment": "42",
    "postalCode": "123456"
  },
  "paymentMethod": "BALANCE",
  "paymentStatus": "PAID",
  "createdAt": "2025-05-01T15:30:00",
  "appliedPromoCode": "SUMMER2025"
}
```

### Получение списка заказов пользователя

**Запрос:**
```http
GET /api/orders?page=0&size=10&sort=createdAt,desc
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "orderNumber": "ORD-12345678",
      "status": "NEW",
      "total": 6350.00,
      "createdAt": "2025-05-01T15:30:00",
      "paymentStatus": "PAID"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true,
  "first": true,
  "empty": false
}
```

### Получение заказа по ID

**Запрос:**
```http
GET /api/orders/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ (200 OK):**
```json
{
  "id": 1,
  "orderNumber": "ORD-12345678",
  "status": "NEW",
  "items": [
    {
      "id": 1,
      "productId": 1,
      "productName": "Футболка с логотипом",
      "size": "M",
      "color": "черный",
      "quantity": 2,
      "price": 1500.00,
      "totalPrice": 3000.00
    },
    {
      "id": 2,
      "productId": 2,
      "productName": "Толстовка базовая",
      "size": "L",
      "color": "серый",
      "quantity": 1,
      "price": 3500.00,
      "totalPrice": 3500.00
    }
  ],
  "subtotal": 6500.00,
  "discount": 650.00,
  "deliveryPrice": 500.00,
  "total": 6350.00,
  "deliveryAddress": {
    "country": "Россия",
    "city": "Москва",
    "street": "Ленина",
    "house": "10",
    "apartment": "42",
    "postalCode": "123456"
  },
  "paymentMethod": "BALANCE",
  "paymentStatus": "PAID",
  "createdAt": "2025-05-01T15:30:00",
  "updatedAt": "2025-05-01T15:30:00",
  "appliedPromoCode": "SUMMER2025"
}
```

### Отмена заказа

**Запрос:**
```http
POST /api/orders/1/cancel
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "reason": "Изменил решение"
}
```

**Ответ (200 OK):**
```json
{
  "id": 1,
  "orderNumber": "ORD-12345678",
  "status": "CANCELLED",
  "cancelReason": "Изменил решение",
  "cancelledAt": "2025-05-01T16:00:00"
}
```

## Платежи и баланс

### Получение баланса пользователя

**Запрос:**
```http
GET /api/payments/balance
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ (200 OK):**
```json
{
  "balance": 1000.00,
  "pendingDeposits": 500.00,
  "currency": "RUB"
}
```

### Создание запроса на пополнение баланса

**Запрос:**
```http
POST /api/payments/deposits
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "amount": 1000.00
}
```

**Ответ (201 Created):**
```json
{
  "transactionId": 1,
  "transactionCode": "TR-01-AB123C-123456",
  "amount": 1000.00,
  "cardNumber": "1234 5678 9012 3456",
  "cardholderName": "IVAN IVANOV",
  "bankName": "SBERBANK",
  "message": "Для пополнения баланса переведите указанную сумму на карту, обязательно указав код транзакции в комментарии к переводу."
}
```

### Отмена запроса на пополнение баланса

**Запрос:**
```http
POST /api/payments/deposits/TR-01-AB123C-123456/cancel
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ (200 OK):**
```json
{
  "transactionId": 1,
  "transactionCode": "TR-01-AB123C-123456",
  "status": "CANCELLED",
  "message": "Запрос на пополнение баланса успешно отменен"
}
```

### Получение истории транзакций

**Запрос:**
```http
GET /api/payments/transactions?page=0&size=10&sort=createdAt,desc
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ (200 OK):**
```json
{
  "content": [
    {
      "id": 1,
      "transactionCode": "TR-01-AB123C-123456",
      "type": "DEPOSIT",
      "amount": 1000.00,
      "status": "PENDING",
      "createdAt": "2025-05-01T14:00:00"
    },
    {
      "id": 2,
      "transactionCode": "TR-01-DE456F-789012",
      "type": "PAYMENT",
      "amount": 6350.00,
      "status": "COMPLETED",
      "orderId": 1,
      "createdAt": "2025-05-01T15:30:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10,
    "sort": {
      "sorted": true,
      "unsorted": false
    }
  },
  "totalElements": 2,
  "totalPages": 1,
  "last": true,
  "first": true,
  "empty": false
}
```

### Получение деталей транзакции

**Запрос:**
```http
GET /api/payments/transactions/1
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ (200 OK):**
```json
{
  "id": 1,
  "transactionCode": "TR-01-AB123C-123456",
  "type": "DEPOSIT",
  "amount": 1000.00,
  "status": "PENDING",
  "createdAt": "2025-05-01T14:00:00",
  "updatedAt": null,
  "orderId": null,
  "adminComment": null
}
```

## Промокоды

### Проверка промокода

**Запрос:**
```http
GET /api/promocodes/check?code=SUMMER2025
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ (200 OK):**
```json
{
  "code": "SUMMER2025",
  "valid": true,
  "discountPercent": 10,
  "description": "Летняя скидка 10%",
  "expiresAt": "2025-08-31T23:59:59"
}
```

### Получение активных промокодов пользователя

**Запрос:**
```http
GET /api/promocodes/active
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ (200 OK):**
```json
[
  {
    "code": "SUMMER2025",
    "discountPercent": 10,
    "description": "Летняя скидка 10%",
    "expiresAt": "2025-08-31T23:59:59"
  },
  {
    "code": "WELCOME15",
    "discountPercent": 15,
    "description": "Скидка для новых пользователей",
    "expiresAt": "2025-12-31T23:59:59"
  }
]
```

## NFT

### Получение списка NFT пользователя

**Запрос:**
```http
GET /api/nfts/me
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ (200 OK):**
```json
[
  {
    "id": 1,
    "tokenId": "NFT-12345",
    "placeholderUri": "https://example.com/placeholder.jpg",
    "isRevealed": false,
    "rarity": null,
    "createdAt": "2025-05-01T10:00:00"
  },
  {
    "id": 2,
    "tokenId": "NFT-67890",
    "placeholderUri": "https://example.com/placeholder.jpg",
    "isRevealed": true,
    "revealedUri": "https://example.com/revealed.jpg",
    "rarity": "RARE",
    "createdAt": "2025-05-01T11:00:00",
    "revealedAt": "2025-05-01T12:00:00"
  }
]
```

### Получение деталей NFT

**Запрос:**
```http
GET /api/nfts/2
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ (200 OK):**
```json
{
  "id": 2,
  "tokenId": "NFT-67890",
  "placeholderUri": "https://example.com/placeholder.jpg",
  "isRevealed": true,
  "revealedUri": "https://example.com/revealed.jpg",
  "rarity": "RARE",
  "description": "Редкий NFT-токен с эксклюзивным дизайном",
  "benefits": ["Скидка 15% на все товары", "Доступ к эксклюзивным коллекциям"],
  "createdAt": "2025-05-01T11:00:00",
  "revealedAt": "2025-05-01T12:00:00"
}
```

### Раскрытие NFT

**Запрос:**
```http
POST /api/nfts/1/reveal
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ (200 OK):**
```json
{
  "id": 1,
  "tokenId": "NFT-12345",
  "placeholderUri": "https://example.com/placeholder.jpg",
  "isRevealed": true,
  "revealedUri": "https://example.com/revealed_unique.jpg",
  "rarity": "COMMON",
  "description": "Обычный NFT-токен с уникальным дизайном",
  "benefits": ["Скидка 5% на следующий заказ"],
  "createdAt": "2025-05-01T10:00:00",
  "revealedAt": "2025-05-01T15:45:00"
}
``` 