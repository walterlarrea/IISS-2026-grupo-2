FROM maven:3.9.16-eclipse-temurin-25 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN mvn -q clean package

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /workspace/target/app.jar /app/app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
