package com.rag.ops.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 文档管理服务启动类
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
public class DocumentServiceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
