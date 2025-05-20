FROM openjdk:21-jdk-slim

WORKDIR /app

COPY wait-for-it.sh wait-for-it.sh
COPY entrypoint.sh entrypoint.sh
COPY target/honey_shop-0.0.1-SNAPSHOT.jar app.jar
COPY src/main/resources/keystore.p12 keystore.p12

RUN chmod +x wait-for-it.sh entrypoint.sh

EXPOSE 8443

ENTRYPOINT ["./entrypoint.sh"]
