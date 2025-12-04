# 开发指南

## 1. 开发环境搭建

### 1.1 前置要求

**必需软件**:
- Java 17+
- Maven 3.8+
- Python 3.10+
- Node.js 18+
- Docker 20.10+
- Docker Compose 2.0+
- Git

**推荐 IDE**:
- IntelliJ IDEA (Java)
- PyCharm / VS Code (Python)
- VS Code (前端)

### 1.2 克隆项目

```bash
git clone <repository-url>
cd rag-ops-qa-assistant
```

### 1.3 配置开发环境

**1. 启动基础设施**:
```bash
docker-compose up -d postgres redis nacos sentinel-dashboard rabbitmq zipkin prometheus grafana minio elasticsearch chroma
```

**2. 配置环境变量**:
```bash
cp .env.example .env
# 编辑 .env 文件，配置必要的环境变量
```

**3. 初始化数据库**:
```bash
docker exec -i postgres psql -U postgres -d rag_db < infrastructure/postgres/init.sql
```

**4. 导入 Nacos 配置**:
```bash
./infrastructure/nacos/init-nacos.sh
```

### 1.4 IDE 配置

**IntelliJ IDEA**:
1. 导入项目为 Maven 项目
2. 设置 JDK 17
3. 启用 Lombok 插件
4. 配置 Code Style（使用项目提供的配置）

**VS Code (Python)**:
1. 安装 Python 扩展
2. 选择 Python 解释器（虚拟环境）
3. 安装 Pylint、Black、isort

**VS Code (前端)**:
1. 安装 ESLint、Prettier 扩展
2. 启用格式化保存

## 2. 项目结构

### 2.1 整体结构

```
rag-ops-qa-assistant/
├── java-services/              # Java 微服务
│   ├── gateway-service/        # API 网关
│   ├── document-service/       # 文档管理
│   ├── session-service/        # 会话管理
│   ├── auth-service/           # 认证授权
│   ├── monitor-service/        # 监控日志
│   └── config-service/         # 配置管理
├── python-services/            # Python 微服务
│   ├── document-processing-service/  # 文档处理
│   ├── rag-query-service/      # RAG 查询
│   ├── embedding-service/      # 嵌入模型
│   └── llm-service/            # 大模型
├── frontend/                   # 前端应用
├── infrastructure/             # 基础设施配置
├── docs/                       # 文档
├── docker-compose.yml          # Docker Compose 配置
└── pom.xml                     # Maven 父 POM
```

### 2.2 Java 服务结构

```
gateway-service/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/rag/ops/gateway/
│   │   │       ├── config/          # 配置类
│   │   │       ├── controller/      # 控制器
│   │   │       ├── filter/          # 过滤器
│   │   │       ├── service/         # 服务层
│   │   │       ├── exception/       # 异常处理
│   │   │       └── GatewayApplication.java
│   │   └── resources/
│   │       ├── application.yml      # 应用配置
│   │       └── bootstrap.yml        # 启动配置
│   └── test/                        # 测试代码
├── Dockerfile
├── pom.xml
└── README.md
```

### 2.3 Python 服务结构

```
document-processing-service/
├── app/
│   ├── __init__.py
│   ├── main.py                 # FastAPI 应用入口
│   ├── config.py               # 配置
│   ├── models.py               # 数据模型
│   ├── routes.py               # 路由
│   ├── document_processor.py  # 文档处理器
│   ├── parsers/                # 文档解析器
│   │   ├── base.py
│   │   ├── pdf_parser.py
│   │   ├── docx_parser.py
│   │   └── ...
│   ├── chunker.py              # 文本分块器
│   ├── embedding_client.py     # 嵌入服务客户端
│   ├── chroma_client.py        # ChromaDB 客户端
│   ├── rabbitmq_consumer.py    # RabbitMQ 消费者
│   ├── celery_app.py           # Celery 应用
│   ├── tasks.py                # Celery 任务
│   ├── nacos_client.py         # Nacos 客户端
│   ├── logging_config.py       # 日志配置
│   └── metrics.py              # 指标采集
├── tests/                      # 测试代码
├── requirements.txt
├── Dockerfile
└── README.md
```

### 2.4 前端结构

```
frontend/
├── src/
│   ├── components/             # 组件
│   ├── pages/                  # 页面
│   ├── services/               # API 服务
│   ├── utils/                  # 工具函数
│   ├── types/                  # TypeScript 类型
│   ├── locales/                # 国际化
│   ├── App.tsx
│   └── main.tsx
├── public/
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

## 3. 开发流程

### 3.1 创建新功能

**1. 创建特性分支**:
```bash
git checkout -b feature/your-feature-name
```

**2. 开发功能**:
- 编写代码
- 编写单元测试
- 编写文档

**3. 提交代码**:
```bash
git add .
git commit -m "feat: add your feature description"
```

**4. 推送代码**:
```bash
git push origin feature/your-feature-name
```

**5. 创建 Pull Request**

### 3.2 代码规范

**Java 代码规范**:
- 遵循 Google Java Style Guide
- 使用 Lombok 简化代码
- 使用 SLF4J 记录日志
- 编写 Javadoc 注释

**Python 代码规范**:
- 遵循 PEP 8
- 使用 Black 格式化代码
- 使用 isort 排序导入
- 编写 Docstring 注释

**前端代码规范**:
- 遵循 Airbnb JavaScript Style Guide
- 使用 ESLint 检查代码
- 使用 Prettier 格式化代码
- 使用 TypeScript 类型注解

### 3.3 提交信息规范

使用 Conventional Commits 规范：

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type**:
- `feat`: 新功能
- `fix`: 修复 Bug
- `docs`: 文档更新
- `style`: 代码格式调整
- `refactor`: 代码重构
- `test`: 测试相关
- `chore`: 构建/工具相关

**示例**:
```
feat(document-service): add batch upload support

- Add batch upload endpoint
- Implement ZIP file extraction
- Add task status tracking

Closes #123
```

## 4. 添加新服务

### 4.1 添加 Java 微服务

**1. 创建服务目录**:
```bash
cd java-services
mkdir new-service
cd new-service
```

**2. 创建 pom.xml**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.rag.ops</groupId>
        <artifactId>rag-ops-qa-assistant</artifactId>
        <version>1.0.0</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>
    
    <artifactId>new-service</artifactId>
    <name>New Service</name>
    
    <dependencies>
        <!-- Spring Boot Starter Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- Nacos Discovery -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>
        
        <!-- Nacos Config -->
        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
        </dependency>
        
        <!-- 其他依赖 -->
    </dependencies>
</project>
```

**3. 创建 Application 类**:
```java
package com.rag.ops.newservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class NewServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NewServiceApplication.class, args);
    }
}
```

**4. 创建 application.yml**:
```yaml
spring:
  application:
    name: new-service
  
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER:localhost:8848}
      config:
        server-addr: ${NACOS_SERVER:localhost:8848}
        file-extension: yaml

server:
  port: 8090

management:
  endpoints:
    web:
      exposure:
        include: "*"
```

**5. 添加到父 POM**:
编辑 `pom.xml`，添加模块：
```xml
<modules>
    <module>java-services/gateway-service</module>
    <!-- 其他模块 -->
    <module>java-services/new-service</module>
</modules>
```

**6. 添加到 Docker Compose**:
编辑 `docker-compose.yml`，添加服务：
```yaml
new-service:
  build: ./java-services/new-service
  ports:
    - "8090:8090"
  environment:
    - NACOS_SERVER=nacos:8848
  depends_on:
    - nacos
```

### 4.2 添加 Python 微服务

**1. 创建服务目录**:
```bash
cd python-services
mkdir new-service
cd new-service
```

**2. 创建项目结构**:
```bash
mkdir app
touch app/__init__.py
touch app/main.py
touch app/config.py
touch app/models.py
touch app/routes.py
touch requirements.txt
touch Dockerfile
```

**3. 创建 main.py**:
```python
from fastapi import FastAPI
from app.config import settings
from app.routes import router
from app.nacos_client import register_service

app = FastAPI(
    title=settings.APP_NAME,
    version=settings.APP_VERSION
)

app.include_router(router)

@app.on_event("startup")
async def startup_event():
    # 注册到 Nacos
    register_service()

@app.get("/health")
async def health_check():
    return {"status": "healthy"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=9010)
```

**4. 创建 requirements.txt**:
```txt
fastapi==0.104.1
uvicorn[standard]==0.24.0
pydantic==2.5.0
pydantic-settings==2.1.0
nacos-sdk-python==0.1.7
prometheus-client==0.19.0
```

**5. 创建 Dockerfile**:
```dockerfile
FROM python:3.10-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app/ ./app/

EXPOSE 9010

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "9010"]
```

**6. 添加到 Docker Compose**:
```yaml
new-service:
  build: ./python-services/new-service
  ports:
    - "9010:9010"
  environment:
    - NACOS_SERVER=nacos:8848
  depends_on:
    - nacos
```

## 5. 添加新功能

### 5.1 添加新的文档格式支持

**1. 创建新的 Parser**:
```python
# app/parsers/excel_parser.py
from app.parsers.base import BaseParser
import pandas as pd

class ExcelParser(BaseParser):
    def parse(self, file_path: str) -> str:
        """解析 Excel 文件"""
        df = pd.read_excel(file_path)
        text = df.to_string()
        return text
```

**2. 注册 Parser**:
```python
# app/parsers/parser_factory.py
from app.parsers.excel_parser import ExcelParser

class ParserFactory:
    @staticmethod
    def get_parser(file_type: str):
        parsers = {
            "pdf": PDFParser(),
            "docx": DOCXParser(),
            "txt": TextParser(),
            "md": MarkdownParser(),
            "xlsx": ExcelParser(),  # 新增
        }
        return parsers.get(file_type)
```

**3. 更新支持的格式列表**:
```python
# app/config.py
SUPPORTED_FORMATS = ["pdf", "docx", "txt", "md", "xlsx"]
```

### 5.2 添加新的 LLM 提供商

**1. 创建新的 Adapter**:
```python
# app/adapters/anthropic_adapter.py
from app.adapters.base import BaseLLMAdapter
import anthropic

class AnthropicAdapter(BaseLLMAdapter):
    def __init__(self, api_key: str):
        self.client = anthropic.Anthropic(api_key=api_key)
    
    def generate(self, prompt: str, **kwargs) -> str:
        response = self.client.messages.create(
            model="claude-3-opus-20240229",
            max_tokens=kwargs.get("max_tokens", 1000),
            messages=[{"role": "user", "content": prompt}]
        )
        return response.content[0].text
    
    def stream_generate(self, prompt: str, **kwargs):
        with self.client.messages.stream(
            model="claude-3-opus-20240229",
            max_tokens=kwargs.get("max_tokens", 1000),
            messages=[{"role": "user", "content": prompt}]
        ) as stream:
            for text in stream.text_stream:
                yield text
```

**2. 注册 Adapter**:
```python
# app/llm_service.py
from app.adapters.anthropic_adapter import AnthropicAdapter

class LLMService:
    def __init__(self):
        self.adapters = {
            "openai": OpenAIAdapter,
            "azure": AzureAdapter,
            "local": LocalAdapter,
            "anthropic": AnthropicAdapter,  # 新增
        }
    
    def get_adapter(self, provider: str):
        adapter_class = self.adapters.get(provider)
        if not adapter_class:
            raise ValueError(f"Unsupported provider: {provider}")
        return adapter_class(api_key=self.config.api_key)
```

### 5.3 添加新的 API 端点

**Java 示例**:
```java
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {
    
    @Autowired
    private DocumentService documentService;
    
    @GetMapping("/search")
    public ResponseEntity<List<Document>> searchDocuments(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        List<Document> documents = documentService.searchDocuments(
            keyword, page, pageSize
        );
        return ResponseEntity.ok(documents);
    }
}
```

**Python 示例**:
```python
@router.get("/documents/search")
async def search_documents(
    keyword: str,
    page: int = 1,
    page_size: int = 20
):
    """搜索文档"""
    documents = await document_service.search_documents(
        keyword, page, page_size
    )
    return {"documents": documents}
```

## 6. 测试

### 6.1 单元测试

**Java 单元测试**:
```java
@SpringBootTest
class DocumentServiceTest {
    
    @Autowired
    private DocumentService documentService;
    
    @Test
    void testUploadDocument() {
        // Given
        MultipartFile file = new MockMultipartFile(
            "file", "test.pdf", "application/pdf", "content".getBytes()
        );
        
        // When
        Document document = documentService.uploadDocument(file);
        
        // Then
        assertNotNull(document.getId());
        assertEquals("test.pdf", document.getFilename());
    }
}
```

**Python 单元测试**:
```python
import pytest
from fastapi.testclient import TestClient
from app.main import app

client = TestClient(app)

def test_upload_document():
    # Given
    files = {"file": ("test.pdf", b"content", "application/pdf")}
    
    # When
    response = client.post("/api/upload", files=files)
    
    # Then
    assert response.status_code == 200
    assert "documentId" in response.json()
```

### 6.2 集成测试

**端到端测试**:
```python
def test_document_upload_and_query():
    # 1. 上传文档
    files = {"file": ("ops-manual.pdf", open("test.pdf", "rb"), "application/pdf")}
    upload_response = client.post("/api/v1/documents/upload", files=files)
    assert upload_response.status_code == 200
    document_id = upload_response.json()["data"]["documentId"]
    
    # 2. 等待处理完成
    time.sleep(10)
    
    # 3. 查询文档
    query_response = client.post(
        "/api/v1/query",
        json={"question": "如何重启服务？"}
    )
    assert query_response.status_code == 200
    assert "answer" in query_response.json()["data"]
```

### 6.3 性能测试

**使用 Locust**:
```python
from locust import HttpUser, task, between

class RAGUser(HttpUser):
    wait_time = between(1, 3)
    
    @task
    def query(self):
        self.client.post(
            "/api/v1/query",
            json={"question": "测试问题"},
            headers={"X-API-Key": "test-key"}
        )
```

运行测试：
```bash
locust -f locustfile.py --host=http://localhost:8080
```

## 7. 调试

### 7.1 本地调试

**Java 服务调试**:
1. 在 IDE 中设置断点
2. 以调试模式运行 Application 类
3. 发送请求触发断点

**Python 服务调试**:
1. 在代码中设置断点
2. 使用 IDE 的调试功能启动服务
3. 或使用 `pdb`:
```python
import pdb; pdb.set_trace()
```

### 7.2 远程调试

**Java 远程调试**:
1. 启动服务时添加 JVM 参数：
```bash
JAVA_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

2. 在 IDE 中配置远程调试，连接到 5005 端口

**Python 远程调试**:
使用 debugpy：
```python
import debugpy
debugpy.listen(("0.0.0.0", 5678))
debugpy.wait_for_client()
```

### 7.3 日志调试

**增加日志级别**:
```yaml
logging:
  level:
    com.rag.ops: DEBUG
```

**查看日志**:
```bash
docker-compose logs -f [service-name]
```

## 8. 代码审查

### 8.1 审查清单

**功能性**:
- [ ] 功能是否符合需求
- [ ] 边界条件是否处理
- [ ] 错误处理是否完善

**代码质量**:
- [ ] 代码是否清晰易读
- [ ] 是否遵循代码规范
- [ ] 是否有重复代码
- [ ] 是否有过度设计

**性能**:
- [ ] 是否有性能问题
- [ ] 数据库查询是否优化
- [ ] 是否有内存泄漏

**安全**:
- [ ] 是否有安全漏洞
- [ ] 输入是否验证
- [ ] 敏感信息是否加密

**测试**:
- [ ] 是否有单元测试
- [ ] 测试覆盖率是否足够
- [ ] 是否有集成测试

**文档**:
- [ ] 是否有代码注释
- [ ] 是否更新文档
- [ ] 是否有 API 文档

## 9. 发布流程

### 9.1 版本管理

使用语义化版本号：`MAJOR.MINOR.PATCH`

- MAJOR: 不兼容的 API 变更
- MINOR: 向后兼容的功能新增
- PATCH: 向后兼容的问题修正

### 9.2 发布步骤

**1. 更新版本号**:
```bash
# 更新 pom.xml
mvn versions:set -DnewVersion=1.1.0

# 更新 package.json
npm version 1.1.0
```

**2. 构建镜像**:
```bash
docker-compose build
```

**3. 打标签**:
```bash
git tag -a v1.1.0 -m "Release version 1.1.0"
git push origin v1.1.0
```

**4. 发布镜像**:
```bash
docker-compose push
```

**5. 部署到生产环境**:
```bash
# 在生产服务器上
docker-compose pull
docker-compose up -d
```

## 10. 最佳实践

### 10.1 代码组织

- 按功能模块组织代码
- 保持类和方法简短
- 使用有意义的命名
- 避免过度设计

### 10.2 错误处理

- 使用统一的异常处理
- 记录详细的错误日志
- 返回友好的错误信息
- 不要吞掉异常

### 10.3 性能优化

- 使用缓存减少重复计算
- 使用异步处理提高吞吐量
- 优化数据库查询
- 使用连接池

### 10.4 安全实践

- 验证所有输入
- 使用参数化查询防止 SQL 注入
- 加密敏感信息
- 使用 HTTPS

### 10.5 文档编写

- 编写清晰的代码注释
- 维护 API 文档
- 编写使用示例
- 更新 README

## 11. 常用命令

### 11.1 Maven 命令

```bash
# 编译
mvn compile

# 测试
mvn test

# 打包
mvn package

# 安装到本地仓库
mvn install

# 清理
mvn clean

# 跳过测试打包
mvn package -DskipTests
```

### 11.2 Docker 命令

```bash
# 构建镜像
docker build -t service-name .

# 运行容器
docker run -d -p 8080:8080 service-name

# 查看日志
docker logs -f container-id

# 进入容器
docker exec -it container-id bash

# 停止容器
docker stop container-id

# 删除容器
docker rm container-id
```

### 11.3 Git 命令

```bash
# 创建分支
git checkout -b feature/new-feature

# 提交代码
git add .
git commit -m "feat: add new feature"

# 推送代码
git push origin feature/new-feature

# 合并分支
git checkout main
git merge feature/new-feature

# 打标签
git tag -a v1.0.0 -m "Release 1.0.0"
git push origin v1.0.0
```

## 12. 资源链接

### 12.1 官方文档

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Cloud Alibaba](https://spring-cloud-alibaba-group.github.io/)
- [FastAPI](https://fastapi.tiangolo.com/)
- [React](https://react.dev/)
- [Docker](https://docs.docker.com/)

### 12.2 工具文档

- [Maven](https://maven.apache.org/)
- [Nacos](https://nacos.io/)
- [Sentinel](https://sentinelguard.io/)
- [Prometheus](https://prometheus.io/)
- [Grafana](https://grafana.com/)

### 12.3 学习资源

- [微服务架构设计模式](https://microservices.io/)
- [12-Factor App](https://12factor.net/)
- [Clean Code](https://www.amazon.com/Clean-Code-Handbook-Software-Craftsmanship/dp/0132350882)

---

如有问题，请参考其他文档或提交 Issue。
