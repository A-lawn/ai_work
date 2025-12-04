package com.rag.ops.session.service;

import com.rag.ops.session.client.RagQueryClient;
import com.rag.ops.session.dto.CreateSessionRequest;
import com.rag.ops.session.entity.Session;
import com.rag.ops.session.exception.SessionNotFoundException;
import com.rag.ops.session.repository.MessageRepository;
import com.rag.ops.session.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private RagQueryClient ragQueryClient;

    @InjectMocks
    private SessionService sessionService;

    @Test
    void testCreateSession_ValidRequest_Success() {
        // Arrange
        CreateSessionRequest request = CreateSessionRequest.builder()
                .userId("user-123")
                .build();

        Session savedSession = Session.builder()
                .id("session-123")
                .userId("user-123")
                .messages(new ArrayList<>())
                .build();

        when(sessionRepository.save(any(Session.class))).thenReturn(savedSession);

        // Act
        var response = sessionService.createSession(request);

        // Assert
        assertNotNull(response);
        assertEquals("session-123", response.getSessionId());
        assertEquals("user-123", response.getUserId());
        verify(sessionRepository, times(1)).save(any(Session.class));
    }

    @Test
    void testGetSessionHistory_ExistingSession_ReturnsHistory() {
        // Arrange
        String sessionId = "session-123";
        Session session = Session.builder()
                .id(sessionId)
                .userId("user-123")
                .messages(new ArrayList<>())
                .build();

        when(sessionRepository.findByIdWithMessages(sessionId)).thenReturn(Optional.of(session));

        // Act
        var response = sessionService.getSessionHistory(sessionId);

        // Assert
        assertNotNull(response);
        assertEquals(sessionId, response.getSessionId());
        assertEquals(0, response.getTotalMessages());
    }

    @Test
    void testGetSessionHistory_NonExistingSession_ThrowsException() {
        // Arrange
        String sessionId = "non-existing";
        when(sessionRepository.findByIdWithMessages(sessionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(SessionNotFoundException.class, 
                () -> sessionService.getSessionHistory(sessionId));
    }

    @Test
    void testDeleteSession_ExistingSession_Success() {
        // Arrange
        String sessionId = "session-123";
        when(sessionRepository.existsById(sessionId)).thenReturn(true);

        // Act
        sessionService.deleteSession(sessionId);

        // Assert
        verify(sessionRepository, times(1)).deleteById(sessionId);
    }

    @Test
    void testDeleteSession_NonExistingSession_ThrowsException() {
        // Arrange
        String sessionId = "non-existing";
        when(sessionRepository.existsById(sessionId)).thenReturn(false);

        // Act & Assert
        assertThrows(SessionNotFoundException.class, 
                () -> sessionService.deleteSession(sessionId));
    }
}
