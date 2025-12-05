#!/bin/bash

# LLM 模型切换脚本
# 快速切换不同的大语言模型提供商

set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}   LLM 模型切换工具${NC}"
echo -e "${GREEN}========================================${NC}\n"

# 显示当前配置
echo -e "${YELLOW}当前配置:${NC}"
if [ -f .env ]; then
    CURRENT_PROVIDER=$(grep "^LLM_PROVIDER=" .env | cut -d'=' -f2)
    echo "LLM 提供商: ${CURRENT_PROVIDER:-未设置}"
else
    echo "未找到 .env 文件"
fi

echo ""
echo -e "${YELLOW}支持的模型提供商:${NC}"
echo "1) 通义千问 (Qwen) - 阿里云"
echo "2) 智谱 AI (GLM) - 智谱AI"
echo "3) 百川智能 (Baichuan)"
echo "4) OpenAI"
echo "5) Azure OpenAI"
echo "6) 本地模型"
echo "0) 退出"
echo ""

read -p "请选择提供商 (0-6): " choice

case $choice in
    1)
        PROVIDER="qwen"
        echo -e "\n${GREEN}配置通义千问 (Qwen)${NC}"
        read -p "请输入 Qwen API Key: " API_KEY
        echo ""
        echo "可用模型:"
        echo "  1) qwen-turbo (快速，性价比高)"
        echo "  2) qwen-plus (平衡)"
        echo "  3) qwen-max (最强性能)"
        echo "  4) qwen-max-longcontext (长文本)"
        read -p "选择模型 (1-4, 默认 1): " model_choice
        case $model_choice in
            2) MODEL="qwen-plus" ;;
            3) MODEL="qwen-max" ;;
            4) MODEL="qwen-max-longcontext" ;;
            *) MODEL="qwen-turbo" ;;
        esac
        
        # 更新 .env
        sed -i.bak "s/^LLM_PROVIDER=.*/LLM_PROVIDER=$PROVIDER/" .env
        sed -i.bak "s/^QWEN_API_KEY=.*/QWEN_API_KEY=$API_KEY/" .env
        sed -i.bak "s/^QWEN_MODEL=.*/QWEN_MODEL=$MODEL/" .env
        ;;
        
    2)
        PROVIDER="zhipu"
        echo -e "\n${GREEN}配置智谱 AI (GLM)${NC}"
        read -p "请输入 Zhipu API Key (格式: api_key.secret): " API_KEY
        echo ""
        echo "可用模型:"
        echo "  1) glm-4 (最新旗舰)"
        echo "  2) glm-4-flash (快速版)"
        echo "  3) glm-3-turbo (经济版)"
        read -p "选择模型 (1-3, 默认 1): " model_choice
        case $model_choice in
            2) MODEL="glm-4-flash" ;;
            3) MODEL="glm-3-turbo" ;;
            *) MODEL="glm-4" ;;
        esac
        
        # 更新 .env
        sed -i.bak "s/^LLM_PROVIDER=.*/LLM_PROVIDER=$PROVIDER/" .env
        sed -i.bak "s/^ZHIPU_API_KEY=.*/ZHIPU_API_KEY=$API_KEY/" .env
        sed -i.bak "s/^ZHIPU_MODEL=.*/ZHIPU_MODEL=$MODEL/" .env
        ;;
        
    3)
        PROVIDER="baichuan"
        echo -e "\n${GREEN}配置百川智能 (Baichuan)${NC}"
        read -p "请输入 Baichuan API Key: " API_KEY
        echo ""
        echo "可用模型:"
        echo "  1) Baichuan2-Turbo (标准版)"
        echo "  2) Baichuan2-Turbo-192k (超长文本)"
        read -p "选择模型 (1-2, 默认 1): " model_choice
        case $model_choice in
            2) MODEL="Baichuan2-Turbo-192k" ;;
            *) MODEL="Baichuan2-Turbo" ;;
        esac
        
        # 更新 .env
        sed -i.bak "s/^LLM_PROVIDER=.*/LLM_PROVIDER=$PROVIDER/" .env
        sed -i.bak "s/^BAICHUAN_API_KEY=.*/BAICHUAN_API_KEY=$API_KEY/" .env
        sed -i.bak "s/^BAICHUAN_MODEL=.*/BAICHUAN_MODEL=$MODEL/" .env
        ;;
        
    4)
        PROVIDER="openai"
        echo -e "\n${GREEN}配置 OpenAI${NC}"
        read -p "请输入 OpenAI API Key: " API_KEY
        read -p "请输入 API Base (默认: https://api.openai.com/v1): " API_BASE
        API_BASE=${API_BASE:-https://api.openai.com/v1}
        echo ""
        echo "可用模型:"
        echo "  1) gpt-4 (最强)"
        echo "  2) gpt-4-turbo (快速)"
        echo "  3) gpt-3.5-turbo (经济)"
        read -p "选择模型 (1-3, 默认 1): " model_choice
        case $model_choice in
            2) MODEL="gpt-4-turbo" ;;
            3) MODEL="gpt-3.5-turbo" ;;
            *) MODEL="gpt-4" ;;
        esac
        
        # 更新 .env
        sed -i.bak "s/^LLM_PROVIDER=.*/LLM_PROVIDER=$PROVIDER/" .env
        sed -i.bak "s|^OPENAI_API_KEY=.*|OPENAI_API_KEY=$API_KEY|" .env
        sed -i.bak "s|^OPENAI_API_BASE=.*|OPENAI_API_BASE=$API_BASE|" .env
        sed -i.bak "s/^OPENAI_MODEL=.*/OPENAI_MODEL=$MODEL/" .env
        ;;
        
    5)
        PROVIDER="azure"
        echo -e "\n${GREEN}配置 Azure OpenAI${NC}"
        read -p "请输入 Azure OpenAI API Key: " API_KEY
        read -p "请输入 Azure OpenAI Endpoint: " ENDPOINT
        read -p "请输入 Deployment Name: " DEPLOYMENT
        read -p "请输入 API Version (默认: 2023-12-01-preview): " API_VERSION
        API_VERSION=${API_VERSION:-2023-12-01-preview}
        
        # 更新 .env
        sed -i.bak "s/^LLM_PROVIDER=.*/LLM_PROVIDER=$PROVIDER/" .env
        sed -i.bak "s/^AZURE_OPENAI_KEY=.*/AZURE_OPENAI_KEY=$API_KEY/" .env
        sed -i.bak "s|^AZURE_OPENAI_ENDPOINT=.*|AZURE_OPENAI_ENDPOINT=$ENDPOINT|" .env
        sed -i.bak "s/^AZURE_OPENAI_DEPLOYMENT=.*/AZURE_OPENAI_DEPLOYMENT=$DEPLOYMENT/" .env
        sed -i.bak "s/^AZURE_OPENAI_API_VERSION=.*/AZURE_OPENAI_API_VERSION=$API_VERSION/" .env
        ;;
        
    6)
        PROVIDER="local"
        echo -e "\n${GREEN}配置本地模型${NC}"
        read -p "请输入本地模型 Endpoint: " ENDPOINT
        read -p "请输入模型名称: " MODEL
        
        # 更新 .env
        sed -i.bak "s/^LLM_PROVIDER=.*/LLM_PROVIDER=$PROVIDER/" .env
        sed -i.bak "s|^LOCAL_MODEL_ENDPOINT=.*|LOCAL_MODEL_ENDPOINT=$ENDPOINT|" .env
        sed -i.bak "s/^LOCAL_MODEL_NAME=.*/LOCAL_MODEL_NAME=$MODEL/" .env
        ;;
        
    0)
        echo "退出"
        exit 0
        ;;
        
    *)
        echo -e "${RED}无效选择${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}✓ 配置已更新${NC}"
echo ""
echo -e "${YELLOW}下一步:${NC}"
echo "1. 重启 LLM Service:"
echo "   docker-compose restart llm-service"
echo ""
echo "2. 或者重启所有服务:"
echo "   ./restart.sh"
echo ""
echo "3. 测试连接:"
echo "   curl -X POST http://localhost:8080/api/v1/config/test-llm"
echo ""
echo -e "${YELLOW}查看详细配置文档:${NC}"
echo "   docs/LLM_CONFIGURATION.md"
