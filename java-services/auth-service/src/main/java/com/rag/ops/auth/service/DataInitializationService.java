package com.rag.ops.auth.service;

import com.rag.ops.auth.entity.Role;
import com.rag.ops.auth.entity.User;
import com.rag.ops.auth.repository.RoleRepository;
import com.rag.ops.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * 数据初始化服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataInitializationService implements CommandLineRunner {
    
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public void run(String... args) {
        log.info("Initializing default data...");
        
        // 创建默认角色
        createDefaultRoles();
        
        // 创建默认管理员用户
        createDefaultAdmin();
        
        log.info("Data initialization completed");
    }
    
    /**
     * 创建默认角色
     */
    private void createDefaultRoles() {
        createRoleIfNotExists("ROLE_ADMIN", "管理员角色");
        createRoleIfNotExists("ROLE_USER", "普通用户角色");
    }
    
    /**
     * 创建角色（如果不存在）
     */
    private void createRoleIfNotExists(String name, String description) {
        if (!roleRepository.existsByName(name)) {
            Role role = Role.builder()
                    .name(name)
                    .description(description)
                    .build();
            roleRepository.save(role);
            log.info("Created role: {}", name);
        }
    }
    
    /**
     * 创建默认管理员用户
     */
    private void createDefaultAdmin() {
        String adminUsername = "admin";
        
        if (!userRepository.existsByUsername(adminUsername)) {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                    .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found"));
            
            Set<Role> roles = new HashSet<>();
            roles.add(adminRole);
            
            User admin = User.builder()
                    .username(adminUsername)
                    .password(passwordEncoder.encode("admin123"))
                    .email("admin@example.com")
                    .fullName("系统管理员")
                    .enabled(true)
                    .accountNonExpired(true)
                    .accountNonLocked(true)
                    .credentialsNonExpired(true)
                    .roles(roles)
                    .build();
            
            userRepository.save(admin);
            log.info("Created default admin user: {} (password: admin123)", adminUsername);
            log.warn("Please change the default admin password in production!");
        }
    }
}
