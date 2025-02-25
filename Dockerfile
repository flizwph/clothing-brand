FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/clothing-brand-0.0.1-SNAPSHOT.jar app.jar


CMD ["sh", "-c", "java -jar app.jar"]
