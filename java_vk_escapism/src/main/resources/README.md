# VK Escapism Bot

## Исправление проблемы с таблицей orders

В проекте обнаружены следующие проблемы с таблицей `orders`:

1. Колонка `user_id` либо отсутствует в таблице, либо содержит NULL значения, но модель описывает её как NOT NULL (ошибка: `столбец "user_id" отношения "orders" содержит значения NULL`)

### Шаги для решения

1. **Временное изменение в модели Order.java**:
   Изменено свойство `nullable` с `false` на `true` для поля `userId`:
   ```java
   @Column(name = "user_id", nullable = true)
   private Long userId;
   ```

2. **Выполнение SQL-запросов для исправления базы данных**:
   Необходимо выполнить SQL запросы из файла `manual_fix_orders.sql` через pgAdmin или другой SQL клиент.
   
   ```sql
   -- Проверить наличие колонки user_id
   SELECT column_name, data_type, is_nullable 
   FROM information_schema.columns 
   WHERE table_name = 'orders' AND column_name = 'user_id';

   -- Если колонка существует, но может содержать NULL, обновите значения:
   UPDATE orders SET user_id = 0 WHERE user_id IS NULL;

   -- Если колонки не существует, создайте ее:
   -- ALTER TABLE orders ADD COLUMN user_id int8;
   -- UPDATE orders SET user_id = 0;

   -- Установите ограничение NOT NULL:
   -- ALTER TABLE orders ALTER COLUMN user_id SET NOT NULL;

   -- Проверка результата:
   SELECT COUNT(*) FROM orders WHERE user_id IS NULL;
   ```

3. **После исправления базы данных**:
   После того как все NULL значения будут заменены на 0, можно вернуть аннотацию обратно:
   ```java
   @Column(name = "user_id", nullable = false)
   private Long userId;
   ```

### Дополнительные изменения для предотвращения проблем в будущем

Для предотвращения подобных проблем в DAO классы добавлены проверки на null значения userId:

```java
if (vkId == null) {
    vkId = 0L;
}
```

Эти изменения есть в:
- OrderDao.saveOrder()
- OrderDao.updateOrderStatus() 
- OrderDao.getLatestOrder() 

Также был добавлен метод safeUserId() для безопасного преобразования между разными типами идентификаторов пользователей. 