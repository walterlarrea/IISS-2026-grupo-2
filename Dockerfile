FROM maven:3.9.16-eclipse-temurin-25 AS build_base

WORKDIR /workspace

COPY pom.xml .
COPY core/pom.xml ./core/
COPY common/pom.xml ./common/
COPY controller/pom.xml ./controller/
COPY publisher/pom.xml ./publisher/
COPY switches-api/pom.xml ./switches-api/

RUN mvn dependency:go-offline -B

COPY . .

RUN mvn clean package -DskipITs=true


# ===============================
# Controlador (API + Engine)
# ===============================

FROM eclipse-temurin:25-jre AS controller

WORKDIR /app

COPY --from=build_base /workspace/controller/target/controller-app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]


# ===============================
# Publisher
# ===============================

FROM eclipse-temurin:25-jre AS publisher

WORKDIR /app

COPY --from=build_base /workspace/publisher/target/publisher-app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]


# ===============================
# Switches API
# ===============================

FROM eclipse-temurin:25-jre AS switches-api

WORKDIR /app

COPY --from=build_base /workspace/switches-api/target/switches-api-app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]