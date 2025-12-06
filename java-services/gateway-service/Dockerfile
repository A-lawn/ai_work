# 多阶段构建 - 构建阶段
FROM maven:3.8-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制父 POM 和所有子模块的 POM(Maven 需要解析所有模块)
COPY pom.xml ./
COPY java-services/gateway-service/pom.xml ./java-services/gateway-service/
COPY java-services/document-service/pom.xml ./java-services/document-service/
COPY java-services/session-service/pom.xml ./java-services/session-service/
COPY java-services/auth-service/pom.xml ./java-services/auth-service/
COPY java-services/monitor-service/pom.xml ./java-services/monitor-service/
COPY java-services/config-service/pom.xml ./java-services/config-service/

# 复制服务源代码
COPY java-services/gateway-service/src ./java-services/gateway-service/src

# 构建应用
RUN mvn clean package -pl java-services/gateway-service -am -Dmaven.test.skip=true

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制构建产物
COPY --from=builder /app/java-services/gateway-service/target/*.jar app.jar

# 暴露端口
EXPOSE 8080

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "-Xms512m", "-Xmx1024m", "app.jar"]
