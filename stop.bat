@echo off
REM 智能运维问答助手 - Windows 停止脚本

echo ==========================================
echo 智能运维问答助手 - 停止中...
echo ==========================================
echo.

REM 停止所有服务
echo [INFO] 停止所有服务...
docker-compose down

echo.
echo [SUCCESS] 所有服务已停止
echo.
echo 如需删除数据卷，请运行: docker-compose down -v
echo.
pause
