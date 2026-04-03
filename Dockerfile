# Build stage — maven image based on Eclipse Temurin (openjdk images on Docker Hub are deprecated/removed)
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src

RUN mvn -B -DskipTests clean package

# Runtime — JRE only (smaller than JDK)
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/target/FinanceHub-1.0.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
