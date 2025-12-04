package com.rag.ops.session.repository;

import com.rag.ops.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 会话仓储接口
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, String> {
    
    /**
     * 根据用户ID查找会话列表
     */
    List<Session> findByUserIdOrderByUpdatedAtDesc(String userId);
    
    /**
     * 查找指定时间之前的会话
     */
    List<Session> findByUpdatedAtBefore(LocalDateTime dateTime);
    
    /**
     * 根据ID查找会话并加载消息
     */
    @Query("SELECT s FROM Session s LEFT JOIN FETCH s.messages WHERE s.id = :id")
    Optional<Session> findByIdWithMessages(@Param("id") String id);
}
