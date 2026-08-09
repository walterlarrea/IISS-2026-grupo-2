FROM maven:3.9-eclipse-temurin-25

WORKDIR /app

COPY src/subscriber/pom.xml .
COPY src/subscriber/src ./src

RUN mvn clean package
RUN mvn dependency:copy-dependencies -DoutputDirectory=target/dependency

CMD ["java", "-cp", "target/classes:target/dependency/*", "org.example.Main"]