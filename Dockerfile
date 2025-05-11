# Базовый образ с Java 21
FROM openjdk:21-jdk-slim

# Устанавливаем рабочую директорию внутри контейнера
WORKDIR /app

# Копируем собранный jar внутрь контейнера
COPY target/honey_shop-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/keystore.p12 keystore.p12

# Открываем порт (если нужен для доступа извне)
EXPOSE 8443

# Команда запуска приложения
ENTRYPOINT ["java", "-jar", "app.jar"]
