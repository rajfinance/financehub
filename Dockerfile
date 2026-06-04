# Build stage — maven image based on Eclipse Temurin (openjdk images on Docker Hub are deprecated/removed)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml ./
COPY src ./src

RUN mvn -B -DskipTests clean package \
    && (test -f target/FinanceHub.jar \
        || cp "$(ls target/FinanceHub-*.jar | grep -v '.original$' | head -1)" target/FinanceHub.jar)

# Runtime — JRE only (smaller than JDK)
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Always copy fixed name (pom version may be 2.26.6 etc.; do not hardcode FinanceHub-1.0.jar)
COPY --from=build /app/target/FinanceHub.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
