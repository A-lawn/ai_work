# End-to-End Verification Guide

This guide provides comprehensive instructions for verifying all components and integrations of the RAG Ops QA Assistant microservices system.

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Detailed Verification Steps](#detailed-verification-steps)
4. [Troubleshooting](#troubleshooting)
5. [Performance Testing](#performance-testing)

## Prerequisites

Before running the verification tests, ensure:

1. Docker and Docker Compose are installed
2. All services are running: `./start.sh`
3. Python 3.10+ is installed (for Python tests)
4. Required Python packages: `pip install -r tests/requirements.txt`

## Quick Start

### Automated Verification

Run the comprehensive verification script:

```bash
chmod +x verify-e2e.sh
./verify-e2e.sh
```

This script will automatically test:
- Docker Compose environment
- Service registration and discovery
- Gateway routing
- Document upload and processing
- Query and answer generation
- Multi-turn conversations
- Monitoring and logging
- Distributed tracing
- Chinese language support
- API security

### Python Integration Tests

Run the Python-based integration tests:

```bash
cd tests/integration
pytest test_e2e_flow.py -v -s
```

## Detailed Verification Steps

### 1. Infrastructure Services

#### Verify Docker Compose Environment

```bash
# Check all services are running
docker-compose ps

# Expected output: All services should show "Up" status
```

#### Verify Nacos (Service Registry)

```bash
# Check Nacos health
curl http://localhost:8848/nacos/v1/console/health/liveness

# Access Nacos Console
# URL: http://localhost:8848/nacos
# Username: nacos
# Password: nacos

# Verify registered services
curl "http://localhost:8848/nacos/v1/ns/instance/list?serviceName=gateway-service"
```

Expected services:
- gateway-service
- document-service
- session-service
- auth-service
- config-service
- monitor-service

#### Verify Sentinel Dashboard

```bash
# Access Sentinel Dashboard
# URL: http://localhost:8858
# Username: sentinel
# Password: sentinel

# Check Sentinel rules
ls -la infrastructure/sentinel/rules/
```

### 2. Gateway and Routing

#### Test Gateway Health

```bash
# Check Gateway health
curl http://localhost:8080/actuator/health

# Expected: {"status":"UP"}
```

#### Test Gateway Routes

```bash
# Test document service route
curl http://localhost:8080/api/v1/documents

# Test session service route
curl http://localhost:8080/api/v1/sessions

# Test config service route
curl http://localhost:8080/api/v1/config
```

### 3. Document Upload and Processing Flow

#### Upload a Test Document

```bash
# Create test document
cat > test_doc.txt << EOF
运维知识库测试文档

这是一个测试文档，包含运维相关的知识。

运维最佳实践：
1. 定期备份数据
2. 监控系统性能
3. 及时更新安全补丁
4. 建立故障响应流程
EOF

# Upload document
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -F "file=@test_doc.txt" \
  -F "metadata={\"description\":\"Test document\"}"

# Save the returned document_id
```

#### Check Document Status

```bash
# Replace {document_id} with actual ID
curl http://localhost:8080/api/v1/documents/{document_id}

# Expected status: "COMPLETED" after processing
```

#### Verify Document Processing

```bash
# Check document service logs
docker-compose logs document-service | tail -50

# Check document processing service logs
docker-compose logs document-processing-service | tail -50

# Verify in ChromaDB
curl http://localhost:8001/api/v1/collections
```

### 4. Query and Answer Generation Flow

#### Create a Session

```bash
# Create new session
curl -X POST http://localhost:8080/api/v1/sessions \
  -H "Content-Type: application/json" \
  -d '{"userId":"test_user"}'

# Save the returned session_id
```

#### Submit a Query

```bash
# Replace {session_id} with actual ID
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "什么是运维最佳实践？",
    "session_id": "{session_id}",
    "top_k": 5
  }'

# Expected: JSON response with "answer" and "sources"
```

#### Test Streaming Query

```bash
# Test streaming response
curl -X POST http://localhost:8080/api/v1/query/stream \
  -H "Content-Type: application/json" \
  -d '{
    "question": "介绍一下Docker",
    "session_id": "{session_id}"
  }'
```

### 5. Multi-turn Conversation

#### Test Conversation Context

```bash
# First query
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "什么是Kubernetes？",
    "session_id": "{session_id}"
  }'

# Follow-up query (should understand context)
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{
    "question": "它有什么优点？",
    "session_id": "{session_id}"
  }'

# Check conversation history
curl http://localhost:8080/api/v1/sessions/{session_id}/history
```

### 6. Knowledge Base Management

#### List Documents

```bash
# Get all documents
curl "http://localhost:8080/api/v1/documents?page=0&size=10"
```

#### Get Document Details

```bash
# Get specific document
curl http://localhost:8080/api/v1/documents/{document_id}
```

#### Delete Document

```bash
# Delete document
curl -X DELETE http://localhost:8080/api/v1/documents/{document_id}

# Verify deletion
curl http://localhost:8080/api/v1/documents/{document_id}
# Expected: 404 Not Found
```

### 7. Configuration Management

#### Get System Configuration

```bash
# Get current configuration
curl http://localhost:8080/api/v1/config
```

#### Update Configuration

```bash
# Update configuration
curl -X PUT http://localhost:8080/api/v1/config \
  -H "Content-Type: application/json" \
  -d '{
    "chunkSize": 512,
    "topK": 5,
    "similarityThreshold": 0.7
  }'
```

#### Test LLM Connection

```bash
# Test LLM connectivity
curl -X POST http://localhost:8080/api/v1/config/test-llm \
  -H "Content-Type: application/json" \
  -d '{
    "provider": "openai",
    "apiKey": "your-api-key",
    "endpoint": "https://api.openai.com/v1"
  }'
```

### 8. Monitoring and Logging

#### Check Prometheus

```bash
# Prometheus health
curl http://localhost:9090/-/healthy

# Access Prometheus UI
# URL: http://localhost:9090

# Query metrics
curl 'http://localhost:9090/api/v1/query?query=up'
```

#### Check Grafana

```bash
# Access Grafana
# URL: http://localhost:3001
# Username: admin
# Password: admin

# Verify dashboards are loaded
curl -u admin:admin http://localhost:3001/api/dashboards/home
```

#### Check Elasticsearch

```bash
# Elasticsearch health
curl http://localhost:9200/_cluster/health

# Search logs
curl http://localhost:9200/logs-*/_search?size=10
```

### 9. Distributed Tracing

#### Check Zipkin

```bash
# Zipkin health
curl http://localhost:9411/health

# Access Zipkin UI
# URL: http://localhost:9411

# Get recent traces
curl http://localhost:9411/api/v2/traces?limit=10
```

#### Verify Trace Propagation

```bash
# Make a request to generate trace
curl http://localhost:8080/api/v1/documents

# Check trace in Zipkin UI
# Should see spans across multiple services
```

### 10. Sentinel Circuit Breaking and Rate Limiting

#### Access Sentinel Dashboard

```bash
# URL: http://localhost:8858
# Username: sentinel
# Password: sentinel
```

#### Configure Flow Rules

1. Navigate to "Flow Control Rules"
2. Add rule for gateway-service
3. Set QPS threshold (e.g., 10)
4. Test by sending multiple requests

#### Test Circuit Breaker

```bash
# Send multiple requests to trigger circuit breaker
for i in {1..100}; do
  curl http://localhost:8080/api/v1/documents
  sleep 0.1
done

# Check Sentinel Dashboard for circuit breaker status
```

#### Verify Dynamic Rule Updates

1. Modify rules in Sentinel Dashboard
2. Rules should be persisted to Nacos
3. Services should pick up new rules automatically

```bash
# Check rules in Nacos
curl "http://localhost:8848/nacos/v1/cs/configs?dataId=gateway-service-flow-rules&group=SENTINEL_GROUP"
```

### 11. Service Scaling

#### Check Current Replicas

```bash
# Check running instances
docker-compose ps | grep -E "(document-service|session-service)"
```

#### Scale Services

```bash
# Scale document service
./scale.sh document-service 3

# Verify scaling
docker-compose ps document-service

# Test load balancing
for i in {1..10}; do
  curl http://localhost:8080/api/v1/documents
done

# Check logs to see requests distributed across instances
```

### 12. Chinese Language Support

#### Upload Chinese Document

```bash
# Create Chinese document
cat > chinese_doc.txt << EOF
中文测试文档

这是一个完全使用中文的测试文档。

运维知识：
- 系统监控
- 日志分析
- 性能优化
- 故障排查
EOF

# Upload
curl -X POST http://localhost:8080/api/v1/documents/upload \
  -F "file=@chinese_doc.txt"
```

#### Query in Chinese

```bash
# Create session
SESSION_ID=$(curl -s -X POST http://localhost:8080/api/v1/sessions \
  -H "Content-Type: application/json" \
  -d '{"userId":"chinese_test"}' | jq -r '.id')

# Query in Chinese
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d "{
    \"question\": \"什么是系统监控？\",
    \"session_id\": \"$SESSION_ID\"
  }"
```

### 13. API Authentication and Security

#### Test Without Authentication

```bash
# Should return 401 or 403 if auth is enabled
curl http://localhost:8080/api/v1/documents
```

#### Login and Get Token

```bash
# Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin123"
  }'

# Save the returned token
```

#### Use Token for Authenticated Requests

```bash
# Use token in header
curl http://localhost:8080/api/v1/documents \
  -H "Authorization: Bearer {token}"
```

### 14. Batch Processing

#### Upload Batch Documents

```bash
# Create ZIP file with multiple documents
mkdir -p /tmp/batch_docs
echo "Document 1" > /tmp/batch_docs/doc1.txt
echo "Document 2" > /tmp/batch_docs/doc2.txt
echo "Document 3" > /tmp/batch_docs/doc3.txt
cd /tmp && zip -r batch_docs.zip batch_docs/

# Upload batch
curl -X POST http://localhost:8080/api/v1/documents/batch-upload \
  -F "file=@/tmp/batch_docs.zip"

# Save the returned task_id
```

#### Check Batch Processing Status

```bash
# Check status
curl http://localhost:8080/api/v1/documents/batch-upload/{task_id}

# Expected: Progress and status information
```

## Troubleshooting

### Services Not Starting

```bash
# Check logs
docker-compose logs --tail=100

# Restart specific service
docker-compose restart {service-name}

# Rebuild and restart
docker-compose up -d --build {service-name}
```

### Service Not Registered in Nacos

```bash
# Check service logs
docker-compose logs {service-name}

# Verify Nacos connectivity
docker-compose exec {service-name} ping nacos

# Check Nacos configuration
docker-compose exec {service-name} cat /app/application.yml
```

### Document Processing Fails

```bash
# Check RabbitMQ
curl http://localhost:15672/api/queues
# Username: admin, Password: admin

# Check document processing service
docker-compose logs document-processing-service

# Check ChromaDB
curl http://localhost:8001/api/v1/collections
```

### Query Returns No Results

```bash
# Check if documents are processed
curl http://localhost:8080/api/v1/documents

# Check ChromaDB collections
curl http://localhost:8001/api/v1/collections

# Check embedding service
docker-compose logs embedding-service

# Check LLM service
docker-compose logs llm-service
```

## Performance Testing

### Run Locust Performance Tests

```bash
# Start Locust
cd tests/performance
locust -f locustfile.py --host=http://localhost:8080

# Access Locust UI
# URL: http://localhost:8089

# Configure:
# - Number of users: 50
# - Spawn rate: 10
# - Run time: 5 minutes
```

### Run Chaos Tests

```bash
# Run chaos engineering tests
cd tests/chaos
python chaos_test.py

# This will:
# - Randomly stop services
# - Introduce network delays
# - Test system resilience
```

### Monitor Performance

```bash
# Check Prometheus metrics
curl 'http://localhost:9090/api/v1/query?query=rate(http_requests_total[5m])'

# Check Grafana dashboards
# URL: http://localhost:3001

# View key metrics:
# - QPS (Queries Per Second)
# - Response time (P50, P95, P99)
# - Error rate
# - Service health
```

## Verification Checklist

Use this checklist to ensure all components are verified:

- [ ] Docker Compose environment is running
- [ ] All services registered in Nacos
- [ ] Sentinel Dashboard accessible and rules configured
- [ ] Gateway routing works correctly
- [ ] Document upload and processing successful
- [ ] Query and answer generation works
- [ ] Multi-turn conversation maintains context
- [ ] Knowledge base management (CRUD) works
- [ ] Configuration management functional
- [ ] Monitoring (Prometheus, Grafana) operational
- [ ] Logging (Elasticsearch) collecting logs
- [ ] Distributed tracing (Zipkin) capturing traces
- [ ] Sentinel circuit breaker triggers correctly
- [ ] Rate limiting enforced
- [ ] Dynamic rule updates work
- [ ] Service scaling functional
- [ ] Chinese language fully supported
- [ ] API authentication enforced
- [ ] Performance meets requirements (< 3s query time)
- [ ] System handles concurrent requests

## Success Criteria

The system is considered fully verified when:

1. **Availability**: All services are up and healthy
2. **Functionality**: All core features work end-to-end
3. **Performance**: Query response time < 3 seconds
4. **Scalability**: System handles 50+ concurrent users
5. **Resilience**: Circuit breakers and fallbacks work
6. **Observability**: Monitoring, logging, and tracing operational
7. **Security**: Authentication and authorization enforced
8. **Internationalization**: Chinese language fully supported

## Next Steps

After successful verification:

1. Review performance metrics and optimize if needed
2. Configure production-ready settings
3. Set up automated monitoring and alerting
4. Document any custom configurations
5. Train operations team on system management
6. Plan for production deployment

## Support

For issues or questions:

- Check logs: `./logs.sh {service-name}`
- Review documentation in `docs/` directory
- Check FAQ: `docs/FAQ.md`
- Review operations guide: `docs/OPERATIONS.md`
