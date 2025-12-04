#!/bin/bash

# 启动 Celery Worker
celery -A app.celery_app worker \
    --loglevel=info \
    --concurrency=2 \
    --queues=batch_processing,document_processing \
    --max-tasks-per-child=100
