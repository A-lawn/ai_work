# 多阶段构建
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 复制父 POM
COPY pom.xml .
COPY java-services/document-service/pom.xml java-services/document-service/

# 下载依赖
RUN mvn dependency:go-offline -pl java-services/document-service

# 复制源代码
COPY java-services/document-service/src java-services/document-service/src

# 构建应用
RUN mvn clean package -pl java-services/document-service -DskipTests

# 运行阶段
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制 JAR 文件
COPY --from=builder /app/java-services/document-service/target/*.jar app.jar

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1

# 暴露端口
EXPOSE 8081

# 启动应用
ENTRYPOINT ["java", "-jar", "app.jar"]
