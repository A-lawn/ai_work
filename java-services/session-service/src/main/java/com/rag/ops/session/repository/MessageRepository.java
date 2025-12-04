package com.rag.ops.session.repository;

import com.rag.ops.session.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 消息仓储接口
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    
    /**
     * 根据会话ID查找消息列表
     */
    @Query("SELECT m FROM Message m WHERE m.session.id = :sessionId ORDER BY m.timestamp ASC")
    List<Message> findBySessionIdOrderByTimestampAsc(@Param("sessionId") String sessionId);
    
    /**
     * 根据会话ID查找最近N条消息
     */
    @Query("SELECT m FROM Message m WHERE m.session.id = :sessionId ORDER BY m.timestamp DESC")
    List<Message> findRecentMessagesBySessionId(@Param("sessionId") String sessionId);
}
