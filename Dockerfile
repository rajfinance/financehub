# Use an official Java runtime as the base image
FROM eclipse-temurin:17-jdk

# Set the working directory inside the container
WORKDIR /app

# Copy the Spring Boot JAR file into the container
COPY target/FinanceHub-1.0.jar app.jar

# Expose the port your app runs on
EXPOSE 8080

# Command to run the JAR file
CMD ["java", "-jar", "app.jar"]
