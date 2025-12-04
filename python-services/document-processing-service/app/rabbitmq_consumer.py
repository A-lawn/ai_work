"""RabbitMQ 消费者模块"""
import json
import logging
import threading
import time
from typing import Callable, Optional
import pika
from pika.exceptions import AMQPConnectionError

from app.config import settings

logger = logging.getLogger(__name__)


class RabbitMQConsumer:
    """RabbitMQ 消费者"""
    
    def __init__(self, callback: Callable):
        """
        初始化 RabbitMQ 消费者
        
        Args:
            callback: 消息处理回调函数
        """
        self.callback = callback
        self.connection: Optional[pika.BlockingConnection] = None
        self.channel: Optional[pika.channel.Channel] = None
        self.consumer_thread: Optional[threading.Thread] = None
        self.is_running = False
        
    def connect(self) -> bool:
        """
        连接到 RabbitMQ
        
        Returns:
            连接是否成功
        """
        try:
            # 解析 RabbitMQ URL
            parameters = pika.URLParameters(settings.rabbitmq_url)
            parameters.heartbeat = 600
            parameters.blocked_connection_timeout = 300
            
            # 建立连接
            self.connection = pika.BlockingConnection(parameters)
            self.channel = self.connection.channel()
            
            # 声明交换机
            self.channel.exchange_declare(
                exchange=settings.rabbitmq_exchange,
                exchange_type='topic',
                durable=True
            )
            
            # 声明队列
            self.channel.queue_declare(
                queue=settings.rabbitmq_queue,
                durable=True
            )
            
            # 绑定队列到交换机
            self.channel.queue_bind(
                exchange=settings.rabbitmq_exchange,
                queue=settings.rabbitmq_queue,
                routing_key=settings.rabbitmq_routing_key
            )
            
            # 设置 QoS（每次只处理一条消息）
            self.channel.basic_qos(prefetch_count=1)
            
            logger.info(
                f"成功连接到 RabbitMQ: {settings.rabbitmq_url}, "
                f"队列: {settings.rabbitmq_queue}"
            )
            return True
            
        except AMQPConnectionError as e:
            logger.error(f"连接 RabbitMQ 失败: {str(e)}")
            return False
        except Exception as e:
            logger.error(f"RabbitMQ 连接异常: {str(e)}")
            return False
    
    def start_consuming(self):
        """开始消费消息（在后台线程中运行）"""
        if self.is_running:
            logger.warning("消费者已在运行中")
            return
        
        self.is_running = True
        self.consumer_thread = threading.Thread(
            target=self._consume_loop,
            daemon=True
        )
        self.consumer_thread.start()
        logger.info("RabbitMQ 消费者已启动")
    
    def _consume_loop(self):
        """消费循环（在后台线程中运行）"""
        retry_count = 0
        max_retries = 5
        
        while self.is_running:
            try:
                # 如果未连接，尝试连接
                if not self.connection or self.connection.is_closed:
                    if not self.connect():
                        retry_count += 1
                        if retry_count >= max_retries:
                            logger.error("达到最大重试次数，停止消费")
                            break
                        
                        wait_time = min(2 ** retry_count, 60)
                        logger.warning(f"等待 {wait_time} 秒后重试连接...")
                        time.sleep(wait_time)
                        continue
                    
                    retry_count = 0
                
                # 开始消费
                self.channel.basic_consume(
                    queue=settings.rabbitmq_queue,
                    on_message_callback=self._on_message,
                    auto_ack=False
                )
                
                logger.info("开始监听消息...")
                self.channel.start_consuming()
                
            except KeyboardInterrupt:
                logger.info("收到中断信号，停止消费")
                break
            except Exception as e:
                logger.error(f"消费循环异常: {str(e)}")
                time.sleep(5)
    
    def _on_message(
        self,
        channel: pika.channel.Channel,
        method: pika.spec.Basic.Deliver,
        properties: pika.spec.BasicProperties,
        body: bytes
    ):
        """
        消息处理回调
        
        Args:
            channel: 通道
            method: 方法
            properties: 属性
            body: 消息体
        """
        try:
            # 解析消息
            message = json.loads(body.decode('utf-8'))
            logger.info(f"收到消息: {message}")
            
            # 调用处理回调
            success = self.callback(message)
            
            if success:
                # 确认消息
                channel.basic_ack(delivery_tag=method.delivery_tag)
                logger.info(f"消息处理成功: {message.get('document_id')}")
            else:
                # 拒绝消息并重新入队
                channel.basic_nack(
                    delivery_tag=method.delivery_tag,
                    requeue=True
                )
                logger.warning(f"消息处理失败，重新入队: {message.get('document_id')}")
                
        except json.JSONDecodeError as e:
            logger.error(f"消息格式错误: {str(e)}")
            # 拒绝消息，不重新入队
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
        except Exception as e:
            logger.error(f"消息处理异常: {str(e)}")
            # 拒绝消息并重新入队
            channel.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
    
    def stop_consuming(self):
        """停止消费消息"""
        self.is_running = False
        
        if self.channel and not self.channel.is_closed:
            try:
                self.channel.stop_consuming()
            except Exception as e:
                logger.warning(f"停止消费时出错: {str(e)}")
        
        if self.connection and not self.connection.is_closed:
            try:
                self.connection.close()
            except Exception as e:
                logger.warning(f"关闭连接时出错: {str(e)}")
        
        logger.info("RabbitMQ 消费者已停止")
    
    def publish_message(self, message: dict, routing_key: str = None):
        """
        发布消息到 RabbitMQ
        
        Args:
            message: 消息内容
            routing_key: 路由键
        """
        if not self.channel or self.channel.is_closed:
            if not self.connect():
                raise Exception("无法连接到 RabbitMQ")
        
        try:
            routing_key = routing_key or settings.rabbitmq_routing_key
            
            self.channel.basic_publish(
                exchange=settings.rabbitmq_exchange,
                routing_key=routing_key,
                body=json.dumps(message, ensure_ascii=False),
                properties=pika.BasicProperties(
                    delivery_mode=2,  # 持久化消息
                    content_type='application/json'
                )
            )
            
            logger.info(f"消息发布成功: routing_key={routing_key}")
            
        except Exception as e:
            logger.error(f"发布消息失败: {str(e)}")
            raise
