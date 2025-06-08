# Этап сборки с готовым Maven
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

# Копируем pom.xml и скачиваем зависимости (для кэширования слоёв)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Копируем исходный код и собираем
COPY src ./src
RUN mvn clean package -DskipTests -B

# Этап выполнения
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Устанавливаем curl для health checks
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Создаём пользователя
RUN groupadd -r appgroup && useradd -r -g appgroup appuser

# Копируем JAR файл
COPY --from=build /app/target/clothing-brand-*.jar app.jar

# Устанавливаем владельца
RUN chown appuser:appgroup app.jar

# Переключаемся на пользователя
USER appuser

# Порт приложения
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# JVM настройки
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseContainerSupport"

# Запуск
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
