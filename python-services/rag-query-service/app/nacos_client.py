"""Nacos 服务注册客户端"""
import logging
import socket
from typing import Optional
from nacos import NacosClient
from app.config import settings

logger = logging.getLogger(__name__)


class NacosRegistry:
    """Nacos 服务注册管理器"""
    
    def __init__(self):
        self.client: Optional[NacosClient] = None
        self.service_ip: Optional[str] = None
        self.registered = False
        
    def _get_local_ip(self) -> str:
        """获取本机 IP 地址"""
        try:
            # 创建一个 UDP socket
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            # 连接到外部地址（不会真正发送数据）
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except Exception:
            return "127.0.0.1"
    
    def register(self) -> bool:
        """注册服务到 Nacos"""
        try:
            # 创建 Nacos 客户端
            self.client = NacosClient(
                server_addresses=settings.nacos_server,
                namespace=settings.nacos_namespace
            )
            
            # 获取服务 IP
            self.service_ip = self._get_local_ip()
            
            # 注册服务实例
            self.client.add_naming_instance(
                service_name=settings.service_name,
                ip=self.service_ip,
                port=settings.service_port,
                group_name=settings.nacos_group,
                metadata={
                    "version": "1.0.0",
                    "service_type": "python",
                    "framework": "fastapi"
                }
            )
            
            self.registered = True
            logger.info(
                f"服务已注册到 Nacos: {settings.service_name} "
                f"({self.service_ip}:{settings.service_port})"
            )
            return True
            
        except Exception as e:
            logger.error(f"注册服务到 Nacos 失败: {str(e)}")
            return False
    
    def deregister(self) -> bool:
        """从 Nacos 注销服务"""
        if not self.client or not self.registered:
            return True
        
        try:
            self.client.remove_naming_instance(
                service_name=settings.service_name,
                ip=self.service_ip,
                port=settings.service_port,
                group_name=settings.nacos_group
            )
            
            self.registered = False
            logger.info(f"服务已从 Nacos 注销: {settings.service_name}")
            return True
            
        except Exception as e:
            logger.error(f"从 Nacos 注销服务失败: {str(e)}")
            return False
    
    def get_service_instance(self, service_name: str) -> Optional[dict]:
        """获取服务实例"""
        if not self.client:
            return None
        
        try:
            instances = self.client.list_naming_instance(
                service_name=service_name,
                group_name=settings.nacos_group,
                healthy_only=True
            )
            
            if instances and len(instances) > 0:
                # 返回第一个健康的实例
                instance = instances[0]
                return {
                    "ip": instance["ip"],
                    "port": instance["port"],
                    "url": f"http://{instance['ip']}:{instance['port']}"
                }
            
            return None
            
        except Exception as e:
            logger.error(f"获取服务实例失败 ({service_name}): {str(e)}")
            return None


# 全局 Nacos 注册实例
nacos_registry = NacosRegistry()
