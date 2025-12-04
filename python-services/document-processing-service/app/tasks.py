"""Celery 异步任务"""
import logging
import traceback
from typing import List, Dict, Any
from celery import Task
from app.celery_app import celery_app
from app.document_processor import DocumentProcessor
from app.config import settings
import redis

logger = logging.getLogger(__name__)

# Redis 客户端用于进度跟踪
redis_client = redis.from_url(settings.redis_url, decode_responses=True)


class CallbackTask(Task):
    """带回调的任务基类"""
    
    def on_success(self, retval, task_id, args, kwargs):
        """任务成功回调"""
        logger.info(f"Task {task_id} succeeded with result: {retval}")
    
    def on_failure(self, exc, task_id, args, kwargs, einfo):
        """任务失败回调"""
        logger.error(f"Task {task_id} failed with exception: {exc}")
        logger.error(f"Traceback: {einfo}")


@celery_app.task(bind=True, base=CallbackTask, name='app.tasks.process_batch_documents')
def process_batch_documents(self, documents_info: List[Dict[str, Any]], task_id: str) -> Dict[str, Any]:
    """
    批量处理文档任务
    
    Args:
        documents_info: 文档信息列表 [{'documentId': str, 'filename': str, 'filePath': str, 'fileType': str}]
        task_id: 批量任务 ID
    
    Returns:
        处理结果报告
    """
    logger.info(f"Starting batch processing task {task_id} with {len(documents_info)} documents")
    
    total = len(documents_info)
    processed = 0
    success_count = 0
    failed_count = 0
    results = []
    
    # 初始化进度
    _update_progress(task_id, {
        'status': 'PROCESSING',
        'total': total,
        'processed': 0,
        'success': 0,
        'failed': 0,
        'progress': 0,
        'results': []
    })
    
    # 创建文档处理器
    from app.document_processor import DocumentProcessor
    processor = DocumentProcessor()
    
    # 逐个处理文档
    for doc_info in documents_info:
        doc_id = doc_info.get('documentId')
        filename = doc_info.get('filename')
        file_path = doc_info.get('filePath')
        file_type = doc_info.get('fileType')
        
        try:
            logger.info(f"Processing document {doc_id} ({filename}) - {processed + 1}/{total}")
            
            # 处理文档（使用同步方法）
            process_result = processor.process_document_sync(
                document_id=doc_id,
                file_path=file_path,
                file_type=file_type,
                filename=filename
            )
            
            result = {
                'document_id': doc_id,
                'filename': filename,
                'status': 'SUCCESS',
                'chunk_count': process_result.get('chunk_count', 0),
                'message': '处理成功'
            }
            
            success_count += 1
            results.append(result)
            
            logger.info(f"Document {doc_id} processed successfully: {result['chunk_count']} chunks")
            
        except Exception as e:
            logger.error(f"Failed to process document {doc_id}: {str(e)}")
            logger.error(traceback.format_exc())
            
            result = {
                'document_id': doc_id,
                'filename': filename,
                'status': 'FAILED',
                'chunk_count': 0,
                'message': str(e)
            }
            
            failed_count += 1
            results.append(result)
        
        finally:
            processed += 1
            progress = int((processed / total) * 100)
            
            # 更新进度
            _update_progress(task_id, {
                'status': 'PROCESSING',
                'total': total,
                'processed': processed,
                'success': success_count,
                'failed': failed_count,
                'progress': progress,
                'results': results
            })
            
            # 更新 Celery 任务状态
            self.update_state(
                state='PROGRESS',
                meta={
                    'current': processed,
                    'total': total,
                    'progress': progress
                }
            )
    
    # 生成最终报告
    final_status = 'COMPLETED' if failed_count == 0 else 'COMPLETED_WITH_ERRORS'
    report = {
        'status': final_status,
        'total': total,
        'processed': processed,
        'success': success_count,
        'failed': failed_count,
        'progress': 100,
        'results': results,
        'summary': f'批量处理完成: 成功 {success_count}, 失败 {failed_count}'
    }
    
    # 保存最终报告
    _update_progress(task_id, report)
    
    logger.info(f"Batch processing task {task_id} completed: {report['summary']}")
    
    return report


@celery_app.task(bind=True, base=CallbackTask, name='app.tasks.process_single_document_task')
def process_single_document_task(
    self,
    document_id: str,
    file_path: str,
    file_type: str
) -> Dict[str, Any]:
    """
    处理单个文档的异步任务
    
    Args:
        document_id: 文档 ID
        file_path: 文件路径
        file_type: 文件类型
    
    Returns:
        处理结果
    """
    logger.info(f"Processing document {document_id} asynchronously")
    
    try:
        processor = DocumentProcessor()
        result = processor.process_document(document_id, file_path, file_type)
        
        logger.info(f"Document {document_id} processed successfully")
        return {
            'success': True,
            'document_id': document_id,
            'chunk_count': result.get('chunk_count', 0),
            'message': '文档处理成功'
        }
        
    except Exception as e:
        logger.error(f"Failed to process document {document_id}: {str(e)}")
        logger.error(traceback.format_exc())
        
        return {
            'success': False,
            'document_id': document_id,
            'message': str(e)
        }


def _update_progress(task_id: str, progress_data: Dict[str, Any]) -> None:
    """
    更新任务进度到 Redis
    
    Args:
        task_id: 任务 ID
        progress_data: 进度数据
    """
    try:
        key = f"batch_task:{task_id}"
        # 将进度数据序列化为 JSON 并存储
        import json
        redis_client.setex(key, 3600, json.dumps(progress_data))  # 保留 1 小时
        logger.debug(f"Progress updated for task {task_id}: {progress_data.get('progress', 0)}%")
    except Exception as e:
        logger.error(f"Failed to update progress for task {task_id}: {str(e)}")
