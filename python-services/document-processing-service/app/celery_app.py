"""Celery 应用配置"""
from celery import Celery
from app.config import settings

# 创建 Celery 应用实例
celery_app = Celery(
    'document_processing',
    broker=settings.redis_url,
    backend=settings.redis_url,
    include=['app.tasks']
)

# Celery 配置
celery_app.conf.update(
    task_serializer='json',
    accept_content=['json'],
    result_serializer='json',
    timezone='Asia/Shanghai',
    enable_utc=True,
    task_track_started=True,
    task_time_limit=30 * 60,  # 30 分钟超时
    task_soft_time_limit=25 * 60,  # 25 分钟软超时
    worker_prefetch_multiplier=1,
    worker_max_tasks_per_child=1000,
    result_expires=3600,  # 结果保留 1 小时
)

# 任务路由配置
celery_app.conf.task_routes = {
    'app.tasks.process_batch_documents': {'queue': 'batch_processing'},
    'app.tasks.process_single_document_task': {'queue': 'document_processing'},
}

if __name__ == '__main__':
    celery_app.start()
