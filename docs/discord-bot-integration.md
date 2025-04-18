# Интеграция с Discord ботом

## Обзор

Для верификации пользователей через Discord используется отдельный Discord-бот, который взаимодействует с основным API через защищенные эндпоинты. Это позволяет пользователям привязывать свои Discord-аккаунты к профилям на сайте нашего бренда.

## Процесс верификации

1. Пользователь может получить код верификации двумя способами:
   - Через сайт: генерирует код на странице профиля
   - Через Telegram бота: отправляет команду `/linkDiscord`

2. После получения кода пользователь:
   - Отправляет команду `!link КОД` боту в Discord (например: `!link a1b2c3d4`)
   - Или отправляет `!link`, а затем код верификации отдельным сообщением
   - Бот проверяет код через API
   - Бот сообщает результат пользователю

3. Коды верификации:
   - Имеют формат 8 символов (например, `a1b2c3d4`)
   - Могут быть использованы только один раз
   - После успешной верификации становятся недействительными

## Интеграция с API

### Настройка переменных окружения

Для работы с API бот должен использовать секретный API-ключ:

```env
API_BASE_URL=https://your-backend-url.com
API_KEY=your-default-secret-key
```

### Пример реализации бота (JDA)

```java
package com.brand.discordbot;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.json.JSONObject;

import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Bot extends ListenerAdapter {
    private final String API_BASE_URL = System.getenv("API_BASE_URL");
    private final String API_KEY = System.getenv("API_KEY");
    private final Map<String, String> userStates = new HashMap<>();

    public static void main(String[] args) throws LoginException {
        JDA jda = JDABuilder.createDefault(System.getenv("DISCORD_TOKEN"))
                .addEventListeners(new Bot())
                .build();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String content = event.getMessage().getContentRaw();
        String userId = event.getAuthor().getId();

        if (content.equals("!verif")) {
            userStates.put(userId, "AWAITING_CODE");
            event.getChannel().sendMessage("Пожалуйста, отправьте код верификации, который вы получили на сайте").queue();
            return;
        }

        if (userStates.getOrDefault(userId, "").equals("AWAITING_CODE")) {
            String code = content.trim();
            verifyCode(event, code);
            userStates.remove(userId);
        }
    }

    private void verifyCode(MessageReceivedEvent event, String code) {
        try {
            URL url = new URL(API_BASE_URL + "/api/discord/verify");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("X-API-KEY", API_KEY);
            conn.setDoOutput(true);

            // Создаем JSON для запроса
            JSONObject requestBody = new JSONObject();
            requestBody.put("code", code);
            requestBody.put("discordId", event.getAuthor().getId());
            requestBody.put("discordUsername", event.getAuthor().getName());

            // Отправляем запрос
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // Получаем ответ
            StringBuilder response = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    conn.getResponseCode() == 200 ? conn.getInputStream() : conn.getErrorStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    response.append(line);
                }
            }

            // Обрабатываем ответ
            if (conn.getResponseCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.toString());
                boolean success = jsonResponse.getBoolean("success");
                String message = jsonResponse.getString("message");

                if (success) {
                    event.getChannel().sendMessage("🎉 " + message).queue();
                } else {
                    event.getChannel().sendMessage("❌ " + message).queue();
                }
            } else {
                event.getChannel().sendMessage("❌ Ошибка сервера: " + conn.getResponseCode()).queue();
            }
        } catch (Exception e) {
            event.getChannel().sendMessage("❌ Ошибка при выполнении запроса: " + e.getMessage()).queue();
        }
    }
}
```

## Эндпоинты API

### Проверка кода верификации
```
POST /api/discord/verify
```

**Заголовки:**
```
Content-Type: application/json
X-API-KEY: {discord_api_secret_key}
```

**Тело запроса:**
```json
{
  "code": "a1b2c3d4",
  "discordId": "123456789",
  "discordUsername": "discord_user"
}
```

**Ответ (успешный):**
```json
{
  "success": true,
  "message": "Discord аккаунт успешно привязан"
}
```

**Ответ (ошибка):**
```json
{
  "success": false,
  "message": "Неверный код верификации"
}
```

### Проверка статуса верификации
```
POST /api/discord/check-status
```

**Заголовки:**
```
Content-Type: application/json
X-API-KEY: {discord_api_secret_key}
```

**Тело запроса:**
```json
{
  "discordId": "123456789",
  "discordUsername": "discord_user"
}
```

**Ответ (пользователь верифицирован):**
```json
{
  "success": true,
  "message": "Ваш аккаунт успешно привязан к сайту! Имя пользователя: username",
  "username": "username",
  "telegramHandle": "@telegram_user"
}
```

**Ответ (пользователь не верифицирован):**
```json
{
  "success": false,
  "message": "Аккаунт не найден. Пожалуйста, завершите верификацию на сайте."
}
```

## Безопасность

1. Всегда используйте HTTPS для подключения к API
2. Никогда не публикуйте API_KEY в открытом доступе
3. Храните API_KEY в переменных окружения
4. Проверяйте, что запрос отправлен настоящим пользователем Discord
5. Не храните долго состояние ожидания кода от пользователя, чтобы избежать утечек памяти

## Обработка ошибок

При возникновении ошибок соединения с API, бот должен:
1. Логировать ошибки для последующего анализа
2. Давать пользователю понятное сообщение
3. Предлагать альтернативные способы решения проблемы

## Рекомендуемые библиотеки

### Java
- JDA (Java Discord API)
- OkHttp для HTTP-запросов
- JSON-P для работы с JSON

### Python
- discord.py
- requests для HTTP-запросов
- pydantic для валидации данных

### Node.js
- discord.js
- axios для HTTP-запросов
- dotenv для работы с переменными окружения 