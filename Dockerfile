FROM  eclipse-temurin:23-jre

WORKDIR /app

COPY target/clothing-brand-0.0.1-SNAPSHOT.jar pisospro.jar

CMD ["java", "-jar", "pisospro.jar"]