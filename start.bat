@echo off
REM 智能运维问答助手 - Windows 启动脚本

echo ==========================================
echo 智能运维问答助手 - 启动中...
echo ==========================================
echo.

REM 检查 Docker 是否安装
docker --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker 未安装，请先安装 Docker Desktop
    pause
    exit /b 1
)

REM 检查 Docker Compose 是否安装
docker-compose --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Docker Compose 未安装，请先安装 Docker Compose
    pause
    exit /b 1
)

REM 检查 .env 文件是否存在
if not exist .env (
    echo [WARNING] .env 文件不存在，从 .env.example 复制...
    copy .env.example .env
    echo 请编辑 .env 文件，配置必要的环境变量（特别是 OPENAI_API_KEY）
    echo 配置完成后，重新运行此脚本
    pause
    exit /b 1
)

REM 启动所有服务
echo [INFO] 启动所有服务...
docker-compose up -d

REM 等待服务启动
echo [INFO] 等待服务启动...
timeout /t 15 /nobreak >nul

REM 检查服务状态
echo.
echo ==========================================
echo 服务状态:
echo ==========================================
docker-compose ps

echo.
echo ==========================================
echo 服务访问地址:
echo ==========================================
echo 前端应用: http://localhost
echo API 网关: http://localhost:8080
echo Nacos 控制台: http://localhost:8848/nacos (nacos/nacos)
echo Sentinel 控制台: http://localhost:8858 (sentinel/sentinel)
echo RabbitMQ 管理界面: http://localhost:15672 (admin/admin123)
echo Zipkin 链路追踪: http://localhost:9411
echo Prometheus: http://localhost:9090
echo Grafana: http://localhost:3001 (admin/admin)
echo MinIO 控制台: http://localhost:9001 (admin/admin123456)
echo ==========================================

echo.
echo [SUCCESS] 所有服务已启动！
echo.
echo 常用命令:
echo   查看日志: docker-compose logs -f [service-name]
echo   停止服务: stop.bat 或 docker-compose down
echo.
pause
