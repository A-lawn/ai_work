# 多阶段构建
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制父 POM 和所有子模块的 POM(Maven 需要解析所有模块)
COPY pom.xml ./
COPY java-services/gateway-service/pom.xml ./java-services/gateway-service/
COPY java-services/document-service/pom.xml ./java-services/document-service/
COPY java-services/session-service/pom.xml ./java-services/session-service/
COPY java-services/auth-service/pom.xml ./java-services/auth-service/
COPY java-services/monitor-service/pom.xml ./java-services/monitor-service/
COPY java-services/config-service/pom.xml ./java-services/config-service/

# 下载依赖
RUN mvn dependency:go-offline -pl java-services/config-service -am

# 复制源代码
COPY java-services/config-service/src java-services/config-service/src

# 构建应用
RUN mvn clean package -pl java-services/config-service -am -Dmaven.test.skip=true

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制构建的JAR文件
COPY --from=builder /app/java-services/config-service/target/*.jar app.jar

# 暴露端口
EXPOSE 8085

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8085/api/health || exit 1

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
