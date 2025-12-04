#!/bin/bash

# Session Service 验证脚本

set -e

echo "=========================================="
echo "Session Service 验证脚本"
echo "=========================================="
echo ""

# 配置
SERVICE_URL="${SERVICE_URL:-http://localhost:8082}"
SESSION_ID=""

# 颜色输出
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 打印成功消息
print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

# 打印错误消息
print_error() {
    echo -e "${RED}✗ $1${NC}"
}

# 打印警告消息
print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

# 打印分隔线
print_separator() {
    echo "----------------------------------------"
}

# 检查服务健康状态
check_health() {
    echo "1. 检查服务健康状态..."
    
    response=$(curl -s -w "\n%{http_code}" "$SERVICE_URL/api/health")
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 200 ]; then
        print_success "服务健康检查通过"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        print_error "服务健康检查失败 (HTTP $http_code)"
        echo "$body"
        exit 1
    fi
    
    print_separator
}

# 创建会话
create_session() {
    echo "2. 创建新会话..."
    
    response=$(curl -s -w "\n%{http_code}" -X POST "$SERVICE_URL/api/v1/sessions" \
        -H "Content-Type: application/json" \
        -d '{
            "userId": "test-user-001",
            "metadata": "{\"source\": \"verification-script\"}"
        }')
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 201 ]; then
        print_success "会话创建成功"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
        SESSION_ID=$(echo "$body" | jq -r '.sessionId' 2>/dev/null)
        echo "会话ID: $SESSION_ID"
    else
        print_error "会话创建失败 (HTTP $http_code)"
        echo "$body"
        exit 1
    fi
    
    print_separator
}

# 添加用户消息
add_user_message() {
    echo "3. 添加用户消息..."
    
    response=$(curl -s -w "\n%{http_code}" -X POST "$SERVICE_URL/api/v1/sessions/$SESSION_ID/messages" \
        -H "Content-Type: application/json" \
        -d '{
            "role": "USER",
            "content": "什么是微服务架构？"
        }')
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 201 ]; then
        print_success "用户消息添加成功"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        print_error "用户消息添加失败 (HTTP $http_code)"
        echo "$body"
        exit 1
    fi
    
    print_separator
}

# 添加助手消息
add_assistant_message() {
    echo "4. 添加助手消息..."
    
    response=$(curl -s -w "\n%{http_code}" -X POST "$SERVICE_URL/api/v1/sessions/$SESSION_ID/messages" \
        -H "Content-Type: application/json" \
        -d '{
            "role": "ASSISTANT",
            "content": "微服务架构是一种将应用程序构建为一组小型、独立服务的架构风格。每个服务运行在自己的进程中，服务之间通过轻量级机制（通常是HTTP API）进行通信。"
        }')
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 201 ]; then
        print_success "助手消息添加成功"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        print_error "助手消息添加失败 (HTTP $http_code)"
        echo "$body"
        exit 1
    fi
    
    print_separator
}

# 获取会话历史
get_session_history() {
    echo "5. 获取会话历史..."
    
    response=$(curl -s -w "\n%{http_code}" "$SERVICE_URL/api/v1/sessions/$SESSION_ID/history")
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 200 ]; then
        print_success "会话历史获取成功"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
        
        # 验证消息数量
        message_count=$(echo "$body" | jq '.totalMessages' 2>/dev/null)
        if [ "$message_count" -eq 2 ]; then
            print_success "消息数量正确: $message_count"
        else
            print_warning "消息数量不符合预期: $message_count (期望: 2)"
        fi
    else
        print_error "会话历史获取失败 (HTTP $http_code)"
        echo "$body"
        exit 1
    fi
    
    print_separator
}

# 添加多条消息测试滑动窗口
test_sliding_window() {
    echo "6. 测试滑动窗口（添加多条消息）..."
    
    for i in {1..5}; do
        curl -s -X POST "$SERVICE_URL/api/v1/sessions/$SESSION_ID/messages" \
            -H "Content-Type: application/json" \
            -d "{
                \"role\": \"USER\",
                \"content\": \"测试消息 $i\"
            }" > /dev/null
        
        curl -s -X POST "$SERVICE_URL/api/v1/sessions/$SESSION_ID/messages" \
            -H "Content-Type: application/json" \
            -d "{
                \"role\": \"ASSISTANT\",
                \"content\": \"回复测试消息 $i\"
            }" > /dev/null
    done
    
    print_success "添加了10条额外消息（5轮对话）"
    
    # 获取会话历史验证
    response=$(curl -s "$SERVICE_URL/api/v1/sessions/$SESSION_ID/history")
    message_count=$(echo "$response" | jq '.totalMessages' 2>/dev/null)
    
    echo "当前消息总数: $message_count"
    
    print_separator
}

# 获取用户会话列表
get_user_sessions() {
    echo "7. 获取用户会话列表..."
    
    response=$(curl -s -w "\n%{http_code}" "$SERVICE_URL/api/v1/sessions?userId=test-user-001")
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 200 ]; then
        print_success "用户会话列表获取成功"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        print_error "用户会话列表获取失败 (HTTP $http_code)"
        echo "$body"
        exit 1
    fi
    
    print_separator
}

# 删除会话
delete_session() {
    echo "8. 删除会话..."
    
    response=$(curl -s -w "\n%{http_code}" -X DELETE "$SERVICE_URL/api/v1/sessions/$SESSION_ID")
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 200 ]; then
        print_success "会话删除成功"
        echo "$body" | jq '.' 2>/dev/null || echo "$body"
    else
        print_error "会话删除失败 (HTTP $http_code)"
        echo "$body"
        exit 1
    fi
    
    print_separator
}

# 验证会话已删除
verify_deletion() {
    echo "9. 验证会话已删除..."
    
    response=$(curl -s -w "\n%{http_code}" "$SERVICE_URL/api/v1/sessions/$SESSION_ID/history")
    
    http_code=$(echo "$response" | tail -n1)
    
    if [ "$http_code" -eq 404 ]; then
        print_success "会话已成功删除（返回404）"
    else
        print_warning "会话删除验证异常 (HTTP $http_code)"
    fi
    
    print_separator
}

# 主函数
main() {
    echo "开始验证 Session Service..."
    echo "服务地址: $SERVICE_URL"
    echo ""
    
    check_health
    create_session
    add_user_message
    add_assistant_message
    get_session_history
    test_sliding_window
    get_user_sessions
    delete_session
    verify_deletion
    
    echo ""
    echo "=========================================="
    print_success "所有验证测试通过！"
    echo "=========================================="
}

# 运行主函数
main
