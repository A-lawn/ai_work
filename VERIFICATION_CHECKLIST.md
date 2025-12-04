# RAG Ops QA Assistant - Verification Checklist

Use this checklist to systematically verify all components of the system.

## Pre-Verification Setup

- [ ] Docker and Docker Compose installed
- [ ] All services started: `./start.sh`
- [ ] Waited 2-3 minutes for services to initialize
- [ ] Python 3.10+ installed
- [ ] Test dependencies installed: `pip install -r tests/requirements.txt`

## Quick Health Check

Run: `./quick-verify.sh`

- [ ] Gateway health check passes
- [ ] Nacos health check passes
- [ ] Services registered in Nacos (≥3 services)
- [ ] Document upload works
- [ ] Session creation works
- [ ] Query processing works
- [ ] Monitoring stack accessible
- [ ] Distributed tracing operational

## 1. Infrastructure Services ✓

### Nacos (Service Registry & Config Center)
- [ ] Nacos accessible at http://localhost:8848/nacos
- [ ] Login successful (nacos/nacos)
- [ ] All services registered:
  - [ ] gateway-service
  - [ ] document-service
  - [ ] session-service
  - [ ] auth-service
  - [ ] config-service
  - [ ] monitor-service
- [ ] Configuration files loaded
- [ ] Service instances show as healthy

### Sentinel Dashboard
- [ ] Sentinel accessible at http://localhost:8858
- [ ] Login successful (sentinel/sentinel)
- [ ] Flow control rules loaded
- [ ] Degrade rules loaded
- [ ] System protection rules loaded
- [ ] Rules persisted to Nacos

### Message Queue (RabbitMQ)
- [ ] RabbitMQ accessible at http://localhost:15672
- [ ] Login successful (admin/admin)
- [ ] Queues created:
  - [ ] document-processing-queue
  - [ ] batch-processing-queue
- [ ] Exchanges configured
- [ ] Dead letter queues configured

### Databases
- [ ] PostgreSQL running (port 5432)
- [ ] Redis running (port 6379)
- [ ] ChromaDB running (port 8001)
- [ ] Elasticsearch running (port 9200)
- [ ] MinIO running (ports 9000, 9001)

### Monitoring Stack
- [ ] Prometheus accessible at http://localhost:9090
- [ ] Prometheus targets all UP
- [ ] Grafana accessible at http://localhost:3001
- [ ] Grafana login successful (admin/admin)
- [ ] Dashboards loaded:
  - [ ] Service Health Dashboard
  - [ ] QPS and Response Time Dashboard
  - [ ] Sentinel Dashboard
  - [ ] Resource Usage Dashboard
  - [ ] Business Metrics Dashboard

### Distributed Tracing
- [ ] Zipkin accessible at http://localhost:9411
- [ ] Traces being collected
- [ ] Spans show service dependencies

## 2. Gateway Service ✓

- [ ] Gateway health: `curl http://localhost:8080/actuator/health`
- [ ] Gateway metrics: `curl http://localhost:8080/actuator/prometheus`
- [ ] Routes configured correctly
- [ ] Authentication filter active
- [ ] Rate limiting configured
- [ ] Circuit breaker configured
- [ ] Request logging working

## 3. Document Upload and Processing Flow ✓

### Upload Document
```bash
echo "Test document" > test.txt
curl -X POST http://localhost:8080/api/v1/documents/upload -F "file=@test.txt"
```

- [ ] Document uploaded successfully
- [ ] Document ID returned
- [ ] File stored in MinIO
- [ ] Metadata saved to PostgreSQL
- [ ] Message sent to RabbitMQ

### Document Processing
- [ ] Document Processing Service receives message
- [ ] Document parsed correctly
- [ ] Text chunked appropriately
- [ ] Embeddings generated
- [ ] Vectors stored in ChromaDB
- [ ] Document status updated to COMPLETED

### Verification
- [ ] Document appears in list: `curl http://localhost:8080/api/v1/documents`
- [ ] Document details accessible
- [ ] Chunk count correct
- [ ] Processing logs in Elasticsearch

## 4. Query and Answer Generation Flow ✓

### Create Session
```bash
curl -X POST http://localhost:8080/api/v1/sessions \
  -H "Content-Type: application/json" \
  -d '{"userId":"test_user"}'
```

- [ ] Session created successfully
- [ ] Session ID returned
- [ ] Session saved to PostgreSQL
- [ ] Session cached in Redis

### Submit Query
```bash
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question":"测试问题","session_id":"SESSION_ID"}'
```

- [ ] Query accepted
- [ ] Query vectorized (Embedding Service)
- [ ] Similar documents retrieved (ChromaDB)
- [ ] Context built with retrieved chunks
- [ ] LLM generates answer
- [ ] Answer returned with sources
- [ ] Query saved to session history

### Streaming Query
- [ ] Streaming endpoint works
- [ ] SSE events received
- [ ] Answer streamed in chunks
- [ ] Complete answer assembled

## 5. Multi-turn Conversation ✓

- [ ] First query processed
- [ ] Second query uses context from first
- [ ] Session history maintained
- [ ] Context window managed (max 10 turns)
- [ ] Token count controlled (< 4000)
- [ ] History retrievable: `curl http://localhost:8080/api/v1/sessions/SESSION_ID/history`

## 6. Knowledge Base Management ✓

### List Documents
- [ ] `GET /api/v1/documents` returns document list
- [ ] Pagination works
- [ ] Filtering by status works

### Get Document Details
- [ ] `GET /api/v1/documents/{id}` returns details
- [ ] Metadata complete
- [ ] Chunk count accurate

### Delete Document
- [ ] `DELETE /api/v1/documents/{id}` removes document
- [ ] Metadata deleted from PostgreSQL
- [ ] Vectors deleted from ChromaDB
- [ ] File deleted from MinIO
- [ ] Document no longer in list

## 7. Configuration Management ✓

### Get Configuration
- [ ] `GET /api/v1/config` returns configuration
- [ ] All settings present:
  - [ ] chunkSize
  - [ ] topK
  - [ ] similarityThreshold
  - [ ] llmProvider
  - [ ] embeddingProvider

### Update Configuration
- [ ] `PUT /api/v1/config` updates settings
- [ ] Changes persisted to database
- [ ] Changes synced to Nacos Config
- [ ] Services pick up new configuration

### Test LLM Connection
- [ ] `POST /api/v1/config/test-llm` validates connection
- [ ] Returns success/failure status
- [ ] Shows latency information

## 8. Monitoring and Logging ✓

### Prometheus
- [ ] All service metrics collected
- [ ] JVM metrics available
- [ ] HTTP metrics available
- [ ] Custom business metrics available
- [ ] Alerting rules configured

### Grafana
- [ ] All dashboards display data
- [ ] Real-time updates working
- [ ] Alerts configured
- [ ] Notification channels set up

### Elasticsearch
- [ ] Logs being collected
- [ ] Filebeat shipping logs
- [ ] Log search working
- [ ] Log retention configured

### Application Logs
- [ ] Operation logs recorded
- [ ] Performance metrics tracked
- [ ] Error logs captured
- [ ] Audit trail maintained

## 9. Distributed Tracing ✓

- [ ] Traces captured for all requests
- [ ] Trace IDs propagated across services
- [ ] Spans show service call chain
- [ ] Timing information accurate
- [ ] Error traces highlighted
- [ ] Dependencies visualized

## 10. Sentinel Circuit Breaking ✓

### Flow Control
- [ ] QPS limits configured
- [ ] Concurrent thread limits set
- [ ] Rate limiting triggers correctly
- [ ] Blocked requests return 429

### Circuit Breaker
- [ ] Error ratio threshold configured
- [ ] Slow call ratio threshold set
- [ ] Circuit opens on threshold breach
- [ ] Circuit half-opens after timeout
- [ ] Circuit closes when healthy
- [ ] Fallback methods execute

### System Protection
- [ ] CPU threshold configured
- [ ] Load threshold set
- [ ] RT threshold configured
- [ ] Thread count limit set
- [ ] System protection triggers

## 11. Dynamic Rule Updates ✓

- [ ] Rules modifiable in Sentinel Dashboard
- [ ] Changes persisted to Nacos
- [ ] Services receive rule updates
- [ ] Rules take effect immediately
- [ ] No service restart required

## 12. Service Scaling ✓

### Scale Up
```bash
./scale.sh document-service 3
```

- [ ] Additional instances start
- [ ] New instances register in Nacos
- [ ] Load balanced across instances
- [ ] No service disruption

### Scale Down
```bash
./scale.sh document-service 1
```

- [ ] Instances gracefully shutdown
- [ ] Deregistered from Nacos
- [ ] Remaining instances handle load
- [ ] No request failures

## 13. Chinese Language Support ✓

### Upload Chinese Document
```bash
cat > chinese.txt << EOF
中文测试文档
这是中文内容
EOF
curl -X POST http://localhost:8080/api/v1/documents/upload -F "file=@chinese.txt"
```

- [ ] Chinese document uploaded
- [ ] Chinese text parsed correctly
- [ ] Chinese text chunked properly
- [ ] Chinese embeddings generated

### Query in Chinese
```bash
curl -X POST http://localhost:8080/api/v1/query \
  -H "Content-Type: application/json" \
  -d '{"question":"这是什么？","session_id":"SESSION_ID"}'
```

- [ ] Chinese query processed
- [ ] Chinese answer generated
- [ ] Chinese characters display correctly
- [ ] No encoding issues

### Frontend Chinese Support
- [ ] UI displays Chinese correctly
- [ ] Chinese input works
- [ ] Chinese output renders properly
- [ ] No font issues

## 14. API Authentication and Security ✓

### Without Authentication
- [ ] Protected endpoints return 401/403
- [ ] Public endpoints accessible

### With Authentication
- [ ] Login successful: `POST /api/auth/login`
- [ ] JWT token received
- [ ] Token validates correctly
- [ ] Token refresh works
- [ ] Token expiration enforced

### API Key Authentication
- [ ] API key generated
- [ ] API key validates requests
- [ ] Invalid key rejected
- [ ] Expired key rejected

### Security Headers
- [ ] CORS configured correctly
- [ ] Security headers present
- [ ] HTTPS enforced (production)

## 15. Batch Processing ✓

### Upload Batch
```bash
# Create ZIP with multiple files
curl -X POST http://localhost:8080/api/v1/documents/batch-upload \
  -F "file=@batch.zip"
```

- [ ] Batch upload accepted
- [ ] Task ID returned
- [ ] ZIP extracted
- [ ] Documents queued for processing

### Monitor Progress
```bash
curl http://localhost:8080/api/v1/documents/batch-upload/TASK_ID
```

- [ ] Task status retrievable
- [ ] Progress percentage shown
- [ ] Processed count updated
- [ ] Failed documents listed

### Completion
- [ ] All documents processed
- [ ] Processing report generated
- [ ] Success/failure counts accurate
- [ ] Notification sent (if configured)

## 16. Performance Testing ✓

### Response Times
- [ ] Document upload < 5s
- [ ] Query response < 3s
- [ ] Session creation < 500ms
- [ ] Document list < 1s

### Concurrent Load
```bash
cd tests/performance
locust -f locustfile.py --host=http://localhost:8080
```

- [ ] System handles 50+ concurrent users
- [ ] Success rate > 95%
- [ ] No memory leaks
- [ ] No connection pool exhaustion

### Resource Usage
- [ ] CPU usage reasonable (< 80%)
- [ ] Memory usage stable
- [ ] Database connections managed
- [ ] No file descriptor leaks

## 17. Chaos Engineering ✓

```bash
cd tests/chaos
python chaos_test.py
```

- [ ] System survives random service failures
- [ ] Circuit breakers activate
- [ ] Fallbacks execute
- [ ] System recovers automatically
- [ ] No data loss
- [ ] Graceful degradation

## 18. Integration Tests ✓

```bash
cd tests/integration
pytest test_e2e_flow.py -v
```

- [ ] All integration tests pass
- [ ] Document upload flow works
- [ ] Query flow works
- [ ] Auth flow works
- [ ] Multi-service flows work

## 19. Automated Verification ✓

```bash
./verify-e2e.sh
```

- [ ] All automated tests pass
- [ ] No critical failures
- [ ] Warnings addressed
- [ ] Test report generated

## Final Verification

- [ ] All checklist items completed
- [ ] No critical issues remaining
- [ ] Performance meets requirements
- [ ] Security controls verified
- [ ] Documentation reviewed
- [ ] Operations team trained
- [ ] Runbooks prepared
- [ ] Monitoring configured
- [ ] Alerts set up
- [ ] Backup strategy in place

## Sign-off

| Role | Name | Date | Signature |
|------|------|------|-----------|
| Developer | | | |
| QA Engineer | | | |
| DevOps Engineer | | | |
| System Architect | | | |
| Project Manager | | | |

## Notes

Record any issues, workarounds, or special configurations:

```
[Add notes here]
```

## Next Steps

After successful verification:

1. [ ] Deploy to staging environment
2. [ ] Conduct user acceptance testing
3. [ ] Perform security audit
4. [ ] Load test at scale
5. [ ] Plan production deployment
6. [ ] Prepare rollback procedures
7. [ ] Schedule go-live date

---

**Verification Date**: _______________

**System Version**: _______________

**Verified By**: _______________
