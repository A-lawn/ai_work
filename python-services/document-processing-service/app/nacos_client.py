"""Nacos 服务注册客户端"""
import logging
from typing import Optional
import nacos
from app.config import settings

logger = logging.getLogger(__name__)


class NacosServiceRegistry:
    """Nacos 服务注册管理器"""
    
    def __init__(self):
        self.client: Optional[nacos.NacosClient] = None
        self.service_name = settings.service_name
        self.service_ip = "127.0.0.1"  # 在 Docker 中会被容器 IP 替换
        self.service_port = settings.service_port
        
    def connect(self):
        """连接到 Nacos 服务器"""
        try:
            self.client = nacos.NacosClient(
                server_addresses=settings.nacos_server,
                namespace=settings.nacos_namespace,
                username="nacos",
                password="nacos"
            )
            logger.info(f"成功连接到 Nacos 服务器: {settings.nacos_server}")
            return True
        except Exception as e:
            logger.error(f"连接 Nacos 失败: {str(e)}")
            return False
    
    def register(self):
        """注册服务到 Nacos"""
        if not self.client:
            if not self.connect():
                return False
        
        try:
            self.client.add_naming_instance(
                service_name=self.service_name,
                ip=self.service_ip,
                port=self.service_port,
                cluster_name="DEFAULT",
                weight=1.0,
                metadata={
                    "version": "1.0.0",
                    "service_type": "python",
                    "framework": "fastapi"
                },
                enable=True,
                healthy=True,
                ephemeral=True
            )
            logger.info(
                f"服务注册成功: {self.service_name} "
                f"({self.service_ip}:{self.service_port})"
            )
            return True
        except Exception as e:
            logger.error(f"服务注册失败: {str(e)}")
            return False
    
    def deregister(self):
        """从 Nacos 注销服务"""
        if not self.client:
            return
        
        try:
            self.client.remove_naming_instance(
                service_name=self.service_name,
                ip=self.service_ip,
                port=self.service_port
            )
            logger.info(f"服务注销成功: {self.service_name}")
        except Exception as e:
            logger.error(f"服务注销失败: {str(e)}")
    
    def send_heartbeat(self):
        """发送心跳（Nacos SDK 会自动处理）"""
        pass


# 全局 Nacos 客户端实例
nacos_registry = NacosServiceRegistry()
