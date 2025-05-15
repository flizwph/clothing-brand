# Контроллер товаров

## Общая информация
Контроллер товаров предоставляет API для получения информации о доступных товарах в магазине.

- **Базовый путь**: `/api/products`
- **Реализация**: `ProductController.java`

## Эндпоинты

### Получение всех товаров
Возвращает список всех доступных товаров в магазине.

- **URL**: `/api/products`
- **Метод**: `GET`
- **Требует авторизации**: Нет

**Успешный ответ (200 OK)**:
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

**Ошибки**:
- `500 Internal Server Error` - Внутренняя ошибка сервера

### Получение товара по ID
Возвращает информацию о конкретном товаре по его идентификатору.

- **URL**: `/api/products/{id}`
- **Метод**: `GET`
- **Требует авторизации**: Нет

**Параметры пути**:
- `id` - Идентификатор товара

**Успешный ответ (200 OK)**:
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

**Ошибки**:
- `404 Not Found` - Товар не найден
- `500 Internal Server Error` - Внутренняя ошибка сервера

### Получение товаров по размеру
Возвращает список товаров, доступных в указанном размере.

- **URL**: `/api/products/size/{size}`
- **Метод**: `GET`
- **Требует авторизации**: Нет

**Параметры пути**:
- `size` - Размер товара (S, M, L, XL)

**Успешный ответ (200 OK)**:
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

**Ошибки**:
- `400 Bad Request` - Неверный формат размера
- `500 Internal Server Error` - Внутренняя ошибка сервера 