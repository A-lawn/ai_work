#!/bin/bash

# RabbitMQ 初始化脚本
# 创建交换机、队列和死信队列

RABBITMQ_HOST="localhost"
RABBITMQ_PORT="15672"
RABBITMQ_USER="admin"
RABBITMQ_PASS="admin123"

echo "等待 RabbitMQ 服务启动..."
until curl -f -u ${RABBITMQ_USER}:${RABBITMQ_PASS} http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/overview > /dev/null 2>&1; do
    echo "RabbitMQ 尚未就绪，等待 5 秒..."
    sleep 5
done

echo "RabbitMQ 服务已就绪，开始初始化..."

# 创建虚拟主机
echo "创建虚拟主机: rag-system"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/vhosts/rag-system

# 设置虚拟主机权限
echo "设置虚拟主机权限"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
    -H "Content-Type: application/json" \
    -d '{"configure":".*","write":".*","read":".*"}' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/permissions/rag-system/${RABBITMQ_USER}

# 创建死信交换机
echo "创建死信交换机: dlx.exchange"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
    -H "Content-Type: application/json" \
    -d '{"type":"topic","durable":true}' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/exchanges/rag-system/dlx.exchange

# 创建死信队列
echo "创建死信队列: dlx.queue"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
    -H "Content-Type: application/json" \
    -d '{"durable":true}' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/rag-system/dlx.queue

# 绑定死信队列到死信交换机
echo "绑定死信队列到死信交换机"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
    -H "Content-Type: application/json" \
    -d '{"routing_key":"#"}' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/rag-system/e/dlx.exchange/q/dlx.queue

# 创建文档处理交换机
echo "创建文档处理交换机: document.exchange"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
    -H "Content-Type: application/json" \
    -d '{"type":"topic","durable":true}' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/exchanges/rag-system/document.exchange

# 创建文档处理队列（带死信配置）
echo "创建文档处理队列: document.processing.queue"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
    -H "Content-Type: application/json" \
    -d '{
        "durable":true,
        "arguments":{
            "x-dead-letter-exchange":"dlx.exchange",
            "x-dead-letter-routing-key":"document.processing.failed",
            "x-message-ttl":300000,
            "x-max-length":10000
        }
    }' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/rag-system/document.processing.queue

# 绑定文档处理队列到交换机
echo "绑定文档处理队列到交换机"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
    -H "Content-Type: application/json" \
    -d '{"routing_key":"document.processing"}' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/rag-system/e/document.exchange/q/document.processing.queue

# 创建文档处理完成队列
echo "创建文档处理完成队列: document.completed.queue"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
    -H "Content-Type: application/json" \
    -d '{
        "durable":true,
        "arguments":{
            "x-dead-letter-exchange":"dlx.exchange",
            "x-dead-letter-routing-key":"document.completed.failed"
        }
    }' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/rag-system/document.completed.queue

# 绑定文档处理完成队列到交换机
echo "绑定文档处理完成队列到交换机"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
    -H "Content-Type: application/json" \
    -d '{"routing_key":"document.completed"}' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/rag-system/e/document.exchange/q/document.completed.queue

# 创建批量处理交换机
echo "创建批量处理交换机: batch.exchange"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
    -H "Content-Type: application/json" \
    -d '{"type":"topic","durable":true}' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/exchanges/rag-system/batch.exchange

# 创建批量处理队列
echo "创建批量处理队列: batch.processing.queue"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
    -H "Content-Type: application/json" \
    -d '{
        "durable":true,
        "arguments":{
            "x-dead-letter-exchange":"dlx.exchange",
            "x-dead-letter-routing-key":"batch.processing.failed",
            "x-message-ttl":600000
        }
    }' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/rag-system/batch.processing.queue

# 绑定批量处理队列到交换机
echo "绑定批量处理队列到交换机"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
    -H "Content-Type: application/json" \
    -d '{"routing_key":"batch.processing"}' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/rag-system/e/batch.exchange/q/batch.processing.queue

# 创建日志事件交换机
echo "创建日志事件交换机: log.exchange"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
    -H "Content-Type: application/json" \
    -d '{"type":"topic","durable":true}' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/exchanges/rag-system/log.exchange

# 创建日志事件队列
echo "创建日志事件队列: log.event.queue"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X PUT \
    -H "Content-Type: application/json" \
    -d '{"durable":true}' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/queues/rag-system/log.event.queue

# 绑定日志事件队列到交换机
echo "绑定日志事件队列到交换机"
curl -u ${RABBITMQ_USER}:${RABBITMQ_PASS} -X POST \
    -H "Content-Type: application/json" \
    -d '{"routing_key":"log.#"}' \
    http://${RABBITMQ_HOST}:${RABBITMQ_PORT}/api/bindings/rag-system/e/log.exchange/q/log.event.queue

echo "RabbitMQ 初始化完成！"
echo ""
echo "创建的交换机："
echo "  - dlx.exchange (死信交换机)"
echo "  - document.exchange (文档处理交换机)"
echo "  - batch.exchange (批量处理交换机)"
echo "  - log.exchange (日志事件交换机)"
echo ""
echo "创建的队列："
echo "  - dlx.queue (死信队列)"
echo "  - document.processing.queue (文档处理队列)"
echo "  - document.completed.queue (文档处理完成队列)"
echo "  - batch.processing.queue (批量处理队列)"
echo "  - log.event.queue (日志事件队列)"
