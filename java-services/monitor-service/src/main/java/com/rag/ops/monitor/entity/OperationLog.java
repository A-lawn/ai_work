package com.rag.ops.monitor.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Operation Log Entity
 * 操作日志实体
 */
@Entity
@Table(name = "operation_logs", indexes = {
    @Index(name = "idx_operation_type", columnList = "operation_type"),
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_timestamp", columnList = "timestamp"),
    @Index(name = "idx_service_name", columnList = "service_name")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "operation_type", nullable = false, length = 50)
    private String operationType;

    @Column(name = "service_name", length = 100)
    private String serviceName;

    @Column(name = "user_id", length = 100)
    private String userId;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "action", length = 50)
    private String action;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", columnDefinition = "jsonb")
    private Map<String, Object> details;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "trace_id", length = 100)
    private String traceId;

    @Column(name = "span_id", length = 100)
    private String spanId;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
