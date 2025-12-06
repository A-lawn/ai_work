# Multi-stage build for Monitor Service

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# 复制父 POM 和所有子模块的 POM(Maven 需要解析所有模块)
COPY pom.xml ./
COPY java-services/gateway-service/pom.xml ./java-services/gateway-service/
COPY java-services/document-service/pom.xml ./java-services/document-service/
COPY java-services/session-service/pom.xml ./java-services/session-service/
COPY java-services/auth-service/pom.xml ./java-services/auth-service/
COPY java-services/monitor-service/pom.xml ./java-services/monitor-service/
COPY java-services/config-service/pom.xml ./java-services/config-service/

# Download dependencies
RUN mvn dependency:go-offline -pl java-services/monitor-service -am

# Copy source code
COPY java-services/monitor-service/src ./java-services/monitor-service/src

# Build application
RUN mvn clean package -pl java-services/monitor-service -am -Dmaven.test.skip=true

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install curl for health checks
RUN apk add --no-cache curl

# Copy jar from build stage
COPY --from=build /app/java-services/monitor-service/target/*.jar app.jar

# Create logs directory
RUN mkdir -p /app/logs

# Expose port
EXPOSE 8084

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8084/api/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "app.jar"]
