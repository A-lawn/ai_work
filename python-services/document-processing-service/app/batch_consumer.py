"""批量文档处理 RabbitMQ 消费者"""
import json
import logging
import pika
from typing import Dict, Any
from app.config import settings
from app.tasks import process_batch_documents

logger = logging.getLogger(__name__)


class BatchProcessingConsumer:
    """批量处理消费者"""
    
    def __init__(self):
        """初始化消费者"""
        self.connection = None
        self.channel = None
        self.queue_name = "document.batch.processing.queue"
        
    def connect(self):
        """连接到 RabbitMQ"""
        try:
            # 解析 RabbitMQ URL
            parameters = pika.URLParameters(settings.rabbitmq_url)
            self.connection = pika.BlockingConnection(parameters)
            self.channel = self.connection.channel()
            
            # 声明队列
            self.channel.queue_declare(
                queue=self.queue_name,
                durable=True,
                arguments={'x-message-ttl': 7200000}  # 2小时过期
            )
            
            logger.info(f"Connected to RabbitMQ, listening on queue: {self.queue_name}")
            
        except Exception as e:
            logger.error(f"Failed to connect to RabbitMQ: {str(e)}")
            raise
    
    def callback(self, ch, method, properties, body):
        """处理消息回调"""
        try:
            # 解析消息
            message = json.loads(body)
            logger.info(f"Received batch processing message: {message}")
            
            task_id = message.get('taskId')
            documents = message.get('documents', [])
            
            if not task_id or not documents:
                logger.error("Invalid message format: missing taskId or documents")
                ch.basic_ack(delivery_tag=method.delivery_tag)
                return
            
            logger.info(f"Starting batch processing for task {task_id} with {len(documents)} documents")
            
            # 异步调用 Celery 任务
            process_batch_documents.delay(documents, task_id)
            
            # 确认消息
            ch.basic_ack(delivery_tag=method.delivery_tag)
            logger.info(f"Batch processing task queued for task {task_id}")
            
        except json.JSONDecodeError as e:
            logger.error(f"Failed to parse message: {str(e)}")
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=False)
            
        except Exception as e:
            logger.error(f"Error processing batch message: {str(e)}")
            # 重新入队
            ch.basic_nack(delivery_tag=method.delivery_tag, requeue=True)
    
    def start_consuming(self):
        """开始消费消息"""
        try:
            self.channel.basic_qos(prefetch_count=1)
            self.channel.basic_consume(
                queue=self.queue_name,
                on_message_callback=self.callback
            )
            
            logger.info("Waiting for batch processing messages...")
            self.channel.start_consuming()
            
        except KeyboardInterrupt:
            logger.info("Stopping batch consumer...")
            self.stop()
            
        except Exception as e:
            logger.error(f"Error in consumer: {str(e)}")
            raise
    
    def stop(self):
        """停止消费者"""
        if self.channel:
            self.channel.stop_consuming()
        if self.connection:
            self.connection.close()
        logger.info("Batch consumer stopped")


def start_batch_consumer():
    """启动批量处理消费者"""
    consumer = BatchProcessingConsumer()
    consumer.connect()
    consumer.start_consuming()


if __name__ == '__main__':
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
    )
    start_batch_consumer()
