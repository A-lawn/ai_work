package com.rag.ops.auth.repository;

import com.rag.ops.auth.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 角色仓储
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {
    
    /**
     * 根据角色名查找角色
     */
    Optional<Role> findByName(String name);
    
    /**
     * 检查角色名是否存在
     */
    boolean existsByName(String name);
}
