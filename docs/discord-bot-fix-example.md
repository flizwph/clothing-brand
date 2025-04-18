# Пример исправления ошибки UserQuestId в Discord боте

## Проблема

В логах Discord бота наблюдается ошибка:
```
org.springframework.orm.jpa.JpaSystemException: Could not set value of type [java.lang.Integer]: 'com.discord.bot.entity.UserQuestId.questId' (setter)
```

Эта ошибка происходит из-за неправильной инициализации поля `questId` в составном ключе `UserQuestId`.

## Решение

### 1. Исправление класса UserQuestId

```java
package com.discord.bot.entity;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class UserQuestId implements Serializable {
    
    private Long userId;
    
    private Integer questId = 0; // Инициализируем значением по умолчанию
    
    public UserQuestId(Long userId) {
        this.userId = userId;
    }
}
```

### 2. Исправление метода в QuestService

```java
@Transactional
public List<UserQuest> getUserQuestsByUserId(Long userId) {
    // Найдем все квесты
    List<Quest> allQuests = questRepository.findAll();
    
    // Найдем все прогрессы пользователя
    List<UserQuest> userQuests = userQuestRepository.findByIdUserId(userId);
    
    // Создадим множество ID квестов, которые уже есть у пользователя
    Set<Integer> existingQuestIds = userQuests.stream()
        .map(uq -> uq.getId().getQuestId())
        .collect(Collectors.toSet());
    
    // Для каждого квеста, который еще не начат пользователем, создадим запись
    for (Quest quest : allQuests) {
        if (!existingQuestIds.contains(quest.getId())) {
            UserQuest userQuest = new UserQuest();
            UserQuestId id = new UserQuestId();
            id.setUserId(userId);
            
            // Важно: Обязательно устанавливаем questId перед сохранением
            id.setQuestId(quest.getId());
            
            userQuest.setId(id);
            userQuest.setProgress(0);
            userQuest.setCompleted(false);
            userQuest.setStartedAt(LocalDateTime.now());
            
            userQuestRepository.save(userQuest);
            userQuests.add(userQuest);
        }
    }
    
    return userQuests;
}
```

## Применение исправления

1. Найдите класс `UserQuestId` в пакете `com.discord.bot.entity`
2. Добавьте инициализацию по умолчанию для поля `questId` как показано выше
3. Добавьте дополнительный конструктор для создания объекта только с userId
4. Найдите метод `getUserQuestsByUserId` в классе `QuestService`
5. Убедитесь, что перед каждым сохранением `UserQuest` полю `questId` всегда присваивается значение 