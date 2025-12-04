# 多阶段构建 - 构建阶段
FROM maven:3.8-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制父 POM
COPY ../../pom.xml ./pom.xml

# 复制服务 POM 和源代码
COPY pom.xml ./gateway-service/pom.xml
COPY src ./gateway-service/src

# 构建应用
WORKDIR /app/gateway-service
RUN mvn clean package -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制构建产物
COPY --from=builder /app/gateway-service/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "-Xms512m", "-Xmx1024m", "app.jar"]
