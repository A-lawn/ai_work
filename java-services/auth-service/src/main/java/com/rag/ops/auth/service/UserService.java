package com.rag.ops.auth.service;

import com.rag.ops.auth.entity.Role;
import com.rag.ops.auth.entity.User;
import com.rag.ops.auth.repository.RoleRepository;
import com.rag.ops.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * 用户服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    
    /**
     * 创建用户
     */
    @Transactional
    public User createUser(String username, String password, String email, String fullName, Set<String> roleNames) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        
        // 检查邮箱是否已存在
        if (email != null && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("邮箱已存在: " + email);
        }
        
        // 获取角色
        Set<Role> roles = new HashSet<>();
        if (roleNames != null && !roleNames.isEmpty()) {
            for (String roleName : roleNames) {
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new IllegalArgumentException("角色不存在: " + roleName));
                roles.add(role);
            }
        } else {
            // 默认角色
            Role userRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        Role newRole = Role.builder()
                                .name("ROLE_USER")
                                .description("普通用户")
                                .build();
                        return roleRepository.save(newRole);
                    });
            roles.add(userRole);
        }
        
        // 创建用户
        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .fullName(fullName)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .roles(roles)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("Created new user: {}", username);
        
        return savedUser;
    }
    
    /**
     * 根据用户名查找用户
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }
    
    /**
     * 根据 ID 查找用户
     */
    public Optional<User> findById(String id) {
        return userRepository.findById(id);
    }
    
    /**
     * 验证用户密码
     */
    public boolean validatePassword(User user, String rawPassword) {
        return passwordEncoder.matches(rawPassword, user.getPassword());
    }
    
    /**
     * 更新最后登录时间
     */
    @Transactional
    public void updateLastLoginTime(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }
    
    /**
     * 更改密码
     */
    @Transactional
    public void changePassword(User user, String newPassword) {
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getUsername());
    }
    
    /**
     * 启用/禁用用户
     */
    @Transactional
    public void setUserEnabled(String userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setEnabled(enabled);
        userRepository.save(user);
        log.info("User {} {}", user.getUsername(), enabled ? "enabled" : "disabled");
    }
}
