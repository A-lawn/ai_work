package com.rag.ops.document.service;

import com.rag.ops.document.config.MinioConfig;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * MinIO 文件存储服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioService {
    
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;
    
    /**
     * 初始化 - 确保 bucket 存在
     */
    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .build()
            );
            
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .build()
                );
                log.info("Created MinIO bucket: {}", minioConfig.getBucketName());
            } else {
                log.info("MinIO bucket already exists: {}", minioConfig.getBucketName());
            }
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket", e);
            throw new RuntimeException("Failed to initialize MinIO", e);
        }
    }
    
    /**
     * 上传文件
     * 
     * @param file 文件
     * @return 文件路径
     */
    public String uploadFile(MultipartFile file) throws IOException {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            
            // 生成唯一文件名
            String objectName = UUID.randomUUID().toString() + extension;
            
            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            
            log.info("File uploaded successfully: {}", objectName);
            return objectName;
            
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new IOException("Failed to upload file", e);
        }
    }
    
    /**
     * 上传文件（字节数组）
     * 
     * @param filename 文件名
     * @param content 文件内容
     * @return 文件路径
     */
    public String uploadFile(String filename, byte[] content) throws IOException {
        try {
            String extension = "";
            if (filename != null && filename.contains(".")) {
                extension = filename.substring(filename.lastIndexOf("."));
            }
            
            // 生成唯一文件名
            String objectName = UUID.randomUUID().toString() + extension;
            
            // 上传文件
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .stream(new java.io.ByteArrayInputStream(content), content.length, -1)
                            .build()
            );
            
            log.info("File uploaded successfully: {}", objectName);
            return objectName;
            
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new IOException("Failed to upload file", e);
        }
    }
    
    /**
     * 下载文件
     * 
     * @param objectName 对象名称
     * @return 文件输入流
     */
    public InputStream downloadFile(String objectName) throws IOException {
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to download file from MinIO: {}", objectName, e);
            throw new IOException("Failed to download file", e);
        }
    }
    
    /**
     * 删除文件
     * 
     * @param objectName 对象名称
     */
    public void deleteFile(String objectName) throws IOException {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
            log.info("File deleted successfully: {}", objectName);
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}", objectName, e);
            throw new IOException("Failed to delete file", e);
        }
    }
    
    /**
     * 检查文件是否存在
     * 
     * @param objectName 对象名称
     * @return 是否存在
     */
    public boolean fileExists(String objectName) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(objectName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
