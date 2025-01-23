FROM maven:3-openjdk-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src

RUN mvn clean package

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN jar tf /app/app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
