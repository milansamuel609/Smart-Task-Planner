# Multi-stage build for Spring Boot application

# Stage 1: Build stage
FROM maven:3.9-amazoncorretto-17 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application (skip tests for faster build)
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM amazoncorretto:17-alpine

# Set working directory
WORKDIR /app

# Copy the jar from build stage
COPY --from=build /app/target/smarttaskplanner-0.0.1-SNAPSHOT.jar .

# Expose the port your app runs on
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "smarttaskplanner-0.0.1-SNAPSHOT.jar"]