"""
Nacos service registration client
"""
import socket
from typing import Optional
import nacos
from app.config import settings
from app.logging_config import get_logger

logger = get_logger(__name__)


class NacosClient:
    """Nacos service registration and discovery client"""
    
    def __init__(self):
        self.client: Optional[nacos.NacosClient] = None
        self.service_ip: Optional[str] = None
        self.service_port: int = settings.SERVICE_PORT
        
    def get_local_ip(self) -> str:
        """
        Get local IP address
        
        Returns:
            Local IP address
        """
        try:
            # Create a socket to get the local IP
            s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            s.connect(("8.8.8.8", 80))
            ip = s.getsockname()[0]
            s.close()
            return ip
        except Exception as e:
            logger.warning(f"Failed to get local IP, using localhost: {e}")
            return "127.0.0.1"
    
    def register_service(self) -> bool:
        """
        Register service with Nacos
        
        Returns:
            True if registration successful, False otherwise
        """
        try:
            # Initialize Nacos client
            self.client = nacos.NacosClient(
                server_addresses=settings.NACOS_SERVER,
                namespace=settings.NACOS_NAMESPACE,
                username="nacos",
                password="nacos"
            )
            
            # Get local IP
            self.service_ip = self.get_local_ip()
            
            # Register service instance
            self.client.add_naming_instance(
                service_name=settings.SERVICE_NAME,
                ip=self.service_ip,
                port=self.service_port,
                group_name=settings.NACOS_GROUP,
                metadata={
                    "version": "1.0.0",
                    "provider": settings.LLM_PROVIDER,
                    "model": settings.OPENAI_MODEL if settings.LLM_PROVIDER == "openai" else settings.LOCAL_MODEL_NAME
                }
            )
            
            logger.info(
                f"Service registered successfully: {settings.SERVICE_NAME} "
                f"at {self.service_ip}:{self.service_port}"
            )
            return True
            
        except Exception as e:
            logger.error(f"Failed to register service with Nacos: {e}")
            return False
    
    def deregister_service(self) -> bool:
        """
        Deregister service from Nacos
        
        Returns:
            True if deregistration successful, False otherwise
        """
        try:
            if self.client and self.service_ip:
                self.client.remove_naming_instance(
                    service_name=settings.SERVICE_NAME,
                    ip=self.service_ip,
                    port=self.service_port,
                    group_name=settings.NACOS_GROUP
                )
                logger.info(f"Service deregistered successfully: {settings.SERVICE_NAME}")
                return True
            return False
            
        except Exception as e:
            logger.error(f"Failed to deregister service from Nacos: {e}")
            return False
    
    def send_heartbeat(self) -> bool:
        """
        Send heartbeat to Nacos
        
        Returns:
            True if heartbeat sent successfully, False otherwise
        """
        try:
            if self.client and self.service_ip:
                self.client.send_heartbeat(
                    service_name=settings.SERVICE_NAME,
                    ip=self.service_ip,
                    port=self.service_port,
                    group_name=settings.NACOS_GROUP
                )
                return True
            return False
            
        except Exception as e:
            logger.error(f"Failed to send heartbeat to Nacos: {e}")
            return False


# Global Nacos client instance
nacos_client = NacosClient()
