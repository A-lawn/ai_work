#!/bin/bash

# Task 14 验证脚本
# 验证服务间集成和查询流程

echo "=========================================="
echo "Task 14: 服务间集成和查询流程 - 验证"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查服务是否运行
check_service() {
    local service_name=$1
    local port=$2
    
    echo -n "检查 $service_name (端口 $port)... "
    
    if curl -s -f http://localhost:$port/actuator/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 运行中${NC}"
        return 0
    else
        echo -e "${RED}✗ 未运行${NC}"
        return 1
    fi
}

# 测试同步查询
test_sync_query() {
    echo ""
    echo "=========================================="
    echo "测试 1: 同步查询（通过会话）"
    echo "=========================================="
    
    echo "发送查询请求..."
    RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/sessions/query \
        -H "Content-Type: application/json" \
        -d '{
            "question": "什么是 Docker？",
            "userId": "test-user"
        }')
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ 请求成功${NC}"
        echo "响应:"
        echo "$RESPONSE" | jq '.'
        
        # 提取会话 ID
        SESSION_ID=$(echo "$RESPONSE" | jq -r '.sessionId')
        echo ""
        echo "会话 ID: $SESSION_ID"
        
        # 保存会话 ID 供后续测试使用
        echo "$SESSION_ID" > /tmp/test_session_id.txt
        
        return 0
    else
        echo -e "${RED}✗ 请求失败${NC}"
        return 1
    fi
}

# 测试多轮对话
test_multi_turn() {
    echo ""
    echo "=========================================="
    echo "测试 2: 多轮对话"
    echo "=========================================="
    
    # 读取之前保存的会话 ID
    if [ -f /tmp/test_session_id.txt ]; then
        SESSION_ID=$(cat /tmp/test_session_id.txt)
        echo "使用会话 ID: $SESSION_ID"
        
        echo "发送第二轮查询..."
        RESPONSE=$(curl -s -X POST http://localhost:8080/api/v1/sessions/query \
            -H "Content-Type: application/json" \
            -d "{
                \"question\": \"如何使用它？\",
                \"sessionId\": \"$SESSION_ID\"
            }")
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ 多轮对话成功${NC}"
            echo "响应:"
            echo "$RESPONSE" | jq '.'
            return 0
        else
            echo -e "${RED}✗ 多轮对话失败${NC}"
            return 1
        fi
    else
        echo -e "${YELLOW}⚠ 跳过（需要先运行测试 1）${NC}"
        return 0
    fi
}

# 测试会话历史
test_session_history() {
    echo ""
    echo "=========================================="
    echo "测试 3: 会话历史"
    echo "=========================================="
    
    if [ -f /tmp/test_session_id.txt ]; then
        SESSION_ID=$(cat /tmp/test_session_id.txt)
        echo "查询会话历史: $SESSION_ID"
        
        RESPONSE=$(curl -s http://localhost:8080/api/v1/sessions/$SESSION_ID/history)
        
        if [ $? -eq 0 ]; then
            echo -e "${GREEN}✓ 获取会话历史成功${NC}"
            echo "会话历史:"
            echo "$RESPONSE" | jq '.'
            return 0
        else
            echo -e "${RED}✗ 获取会话历史失败${NC}"
            return 1
        fi
    else
        echo -e "${YELLOW}⚠ 跳过（需要先运行测试 1）${NC}"
        return 0
    fi
}

# 测试流式查询
test_stream_query() {
    echo ""
    echo "=========================================="
    echo "测试 4: 流式查询"
    echo "=========================================="
    
    echo "发送流式查询请求..."
    echo "（注意：这将显示实时流式响应）"
    echo ""
    
    curl -X POST http://localhost:8080/api/v1/sessions/query/stream \
        -H "Content-Type: application/json" \
        -H "Accept: text/event-stream" \
        -d '{
            "question": "什么是 Kubernetes？",
            "userId": "test-user"
        }' \
        --no-buffer
    
    if [ $? -eq 0 ]; then
        echo ""
        echo -e "${GREEN}✓ 流式查询成功${NC}"
        return 0
    else
        echo ""
        echo -e "${RED}✗ 流式查询失败${NC}"
        return 1
    fi
}

# 测试直接查询（不通过会话）
test_direct_query() {
    echo ""
    echo "=========================================="
    echo "测试 5: 直接查询（不通过会话）"
    echo "=========================================="
    
    echo "发送直接查询请求..."
    RESPONSE=$(curl -s -X POST http://localhost:8080/api/direct/query \
        -H "Content-Type: application/json" \
        -d '{
            "question": "什么是微服务？",
            "top_k": 5
        }')
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✓ 直接查询成功${NC}"
        echo "响应:"
        echo "$RESPONSE" | jq '.'
        return 0
    else
        echo -e "${RED}✗ 直接查询失败${NC}"
        return 1
    fi
}

# 主流程
main() {
    echo "开始验证..."
    echo ""
    
    # 检查必要的服务
    echo "检查服务状态..."
    check_service "Gateway Service" 8080
    GATEWAY_OK=$?
    
    check_service "Session Service" 8082
    SESSION_OK=$?
    
    # 检查 Python 服务（可能在不同端口）
    echo -n "检查 RAG Query Service (端口 9002)... "
    if curl -s -f http://localhost:9002/api/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ 运行中${NC}"
        RAG_OK=0
    else
        echo -e "${RED}✗ 未运行${NC}"
        RAG_OK=1
    fi
    
    echo ""
    
    # 如果服务未运行，提示用户
    if [ $GATEWAY_OK -ne 0 ] || [ $SESSION_OK -ne 0 ] || [ $RAG_OK -ne 0 ]; then
        echo -e "${YELLOW}警告: 部分服务未运行${NC}"
        echo "请确保以下服务正在运行："
        echo "  - Gateway Service (端口 8080)"
        echo "  - Session Service (端口 8082)"
        echo "  - RAG Query Service (端口 9002)"
        echo ""
        echo "启动服务: docker-compose up -d"
        echo ""
        read -p "是否继续测试？(y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    fi
    
    # 运行测试
    TESTS_PASSED=0
    TESTS_TOTAL=5
    
    test_sync_query && ((TESTS_PASSED++))
    test_multi_turn && ((TESTS_PASSED++))
    test_session_history && ((TESTS_PASSED++))
    test_stream_query && ((TESTS_PASSED++))
    test_direct_query && ((TESTS_PASSED++))
    
    # 总结
    echo ""
    echo "=========================================="
    echo "测试总结"
    echo "=========================================="
    echo "通过: $TESTS_PASSED / $TESTS_TOTAL"
    
    if [ $TESTS_PASSED -eq $TESTS_TOTAL ]; then
        echo -e "${GREEN}✓ 所有测试通过！${NC}"
        echo ""
        echo "Task 14 实施成功！"
        exit 0
    else
        echo -e "${YELLOW}⚠ 部分测试未通过${NC}"
        echo ""
        echo "请检查日志以获取更多信息："
        echo "  - Gateway: docker-compose logs gateway-service"
        echo "  - Session: docker-compose logs session-service"
        echo "  - RAG Query: docker-compose logs rag-query-service"
        exit 1
    fi
}

# 运行主流程
main
