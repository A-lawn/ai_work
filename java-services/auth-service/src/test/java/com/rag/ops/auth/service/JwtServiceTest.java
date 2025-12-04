package com.rag.ops.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", 
                "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm");
        ReflectionTestUtils.setField(jwtService, "expirationMs", 3600000L);
    }

    @Test
    void testGenerateToken_ValidUsername_ReturnsToken() {
        // Act
        String token = jwtService.generateToken("testuser");

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    void testExtractUsername_ValidToken_ReturnsUsername() {
        // Arrange
        String username = "testuser";
        String token = jwtService.generateToken(username);

        // Act
        String extractedUsername = jwtService.extractUsername(token);

        // Assert
        assertEquals(username, extractedUsername);
    }

    @Test
    void testValidateToken_ValidToken_ReturnsTrue() {
        // Arrange
        String username = "testuser";
        String token = jwtService.generateToken(username);

        // Act
        boolean isValid = jwtService.validateToken(token, username);

        // Assert
        assertTrue(isValid);
    }

    @Test
    void testValidateToken_WrongUsername_ReturnsFalse() {
        // Arrange
        String token = jwtService.generateToken("testuser");

        // Act
        boolean isValid = jwtService.validateToken(token, "wronguser");

        // Assert
        assertFalse(isValid);
    }
}
