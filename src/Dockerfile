FROM maven:3.9.16-eclipse-temurin-25 AS build_base
WORKDIR /workspace

COPY pom.xml .
COPY subscriber/pom.xml ./subscriber/
COPY publisher/pom.xml ./publisher/

# Go offline to lock in the cached dependencies layer
RUN mvn dependency:go-offline -B

# Copy actual source files and build the whole project
COPY . .
RUN mvn clean package -DskipTests

# ===============================
# Subscriber
# ===============================

FROM eclipse-temurin:25-jre AS subscriber

WORKDIR /app

COPY --from=build_base /workspace/subscriber/target/app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# ===============================
# Publisher
# ===============================

FROM eclipse-temurin:25-jre AS publisher

WORKDIR /app

COPY --from=build_base /workspace/publisher/target/app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]