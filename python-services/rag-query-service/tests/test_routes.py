"""测试 API 端点"""
import pytest
from fastapi.testclient import TestClient
from unittest.mock import patch, AsyncMock
from app.main import app

client = TestClient(app)


class TestQueryEndpoint:
    """测试查询端点"""
    
    def test_health_check(self):
        """测试健康检查端点"""
        # Act
        response = client.get("/api/health")
        
        # Assert
        assert response.status_code == 200
        assert "status" in response.json()
    
    @patch("app.routes.query_engine.process_query", new_callable=AsyncMock)
    def test_query_endpoint_success(self, mock_query):
        """测试查询端点成功"""
        # Arrange
        mock_query.return_value = {
            "answer": "这是答案",
            "sources": [],
            "query_time": 1.5
        }
        
        # Act
        response = client.post("/api/query", json={
            "question": "测试问题",
            "top_k": 5
        })
        
        # Assert
        assert response.status_code == 200
        data = response.json()
        assert "answer" in data
        assert data["answer"] == "这是答案"
    
    def test_query_endpoint_missing_question(self):
        """测试查询端点缺少问题"""
        # Act
        response = client.post("/api/query", json={})
        
        # Assert
        assert response.status_code == 422  # Validation error
