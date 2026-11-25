# Stage 1: Build the Spring Boot application
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven descriptor and download dependencies
COPY pom.xml .
COPY src ./src

# Install maven and package application
RUN apk add --no-cache maven && \
    mvn clean package -DskipTests

# Stage 2: Minimal runtime image
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Run as non-privileged user for container security
RUN addgroup -S sentinel && adduser -S sentinel -G sentinel
USER sentinel

COPY --from=builder /app/target/api-sentinel-1.0.0.jar app.jar

EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-XX:+UseG1GC", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
