FROM maven:3-openjdk-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src

RUN mvn clean install 

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN jar tf /app/your-application.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
