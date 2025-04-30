# Этап сборки приложения
FROM gradle:7.6-jdk17-alpine AS build
WORKDIR /app

# Копируем файлы для сборки
COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
COPY src ./src

# Сборка приложения
RUN gradle build --no-daemon

# Этап выполнения приложения
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Установка необходимых утилит
RUN apk add --no-cache curl jq

# Создание группы и пользователя для запуска приложения
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Копирование JAR из этапа сборки
COPY --from=build /app/build/libs/*.jar app.jar

# Владелец файлов
RUN chown -R appuser:appgroup /app

# Переключение на непривилегированного пользователя
USER appuser

# Проверка здоровья приложения
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Порт приложения
EXPOSE 8080

# Запуск приложения
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
