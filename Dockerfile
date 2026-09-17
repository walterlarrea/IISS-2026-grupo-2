FROM maven:3.9.16-eclipse-temurin-25 AS build_base

WORKDIR /workspace

COPY pom.xml .

COPY subscriber/pom.xml ./subscriber/

COPY publisher/pom.xml ./publisher/

COPY rooms-api/pom.xml ./rooms-api/

COPY switches-api/pom.xml ./switches-api/

RUN mvn dependency:go-offline -B

COPY . .

RUN mvn clean package -DskipTests


# ===============================
# Subscriber
# ===============================

FROM eclipse-temurin:25-jre AS subscriber

WORKDIR /app

COPY --from=build_base /workspace/subscriber/target/subscriber-*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]


# ===============================
# Publisher
# ===============================

FROM eclipse-temurin:25-jre AS publisher

WORKDIR /app

COPY --from=build_base /workspace/publisher/target/publisher-*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]


# ===============================
# Rooms API
# ===============================

FROM eclipse-temurin:25-jre AS rooms-api

WORKDIR /app

COPY --from=build_base /workspace/rooms-api/target/rooms-api-*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]


# ===============================
# Switches API
# ===============================

FROM eclipse-temurin:25-jre AS switches-api

WORKDIR /app

COPY --from=build_base /workspace/switches-api/target/switches-api-*.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]