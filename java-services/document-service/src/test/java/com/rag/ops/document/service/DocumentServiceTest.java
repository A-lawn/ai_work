package com.rag.ops.document.service;

import com.rag.ops.document.client.DocumentProcessingClient;
import com.rag.ops.document.entity.Document;
import com.rag.ops.document.exception.DocumentNotFoundException;
import com.rag.ops.document.exception.InvalidFileException;
import com.rag.ops.document.repository.DocumentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private MinioService minioService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private DocumentProcessingClient documentProcessingClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void testUploadDocument_ValidPdfFile_Success() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        Document savedDocument = Document.builder()
                .id("doc-123")
                .filename("test.pdf")
                .fileType("pdf")
                .status("PROCESSING")
                .build();

        when(minioService.uploadFile(any())).thenReturn("/files/test.pdf");
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

        // Act
        var response = documentService.uploadDocument(file);

        // Assert
        assertNotNull(response);
        assertEquals("doc-123", response.getDocumentId());
        assertEquals("PROCESSING", response.getStatus());
        verify(rabbitTemplate, times(1)).convertAndSend(any(), any(), any());
    }

    @Test
    void testUploadDocument_UnsupportedFormat_ThrowsException() {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.exe",
                "application/octet-stream",
                "test content".getBytes()
        );

        // Act & Assert
        assertThrows(InvalidFileException.class, () -> documentService.uploadDocument(file));
        verify(documentRepository, never()).save(any());
    }

    @Test
    void testGetDocument_ExistingId_ReturnsDocument() {
        // Arrange
        String docId = "doc-123";
        Document document = Document.builder()
                .id(docId)
                .filename("test.pdf")
                .status("COMPLETED")
                .build();

        when(documentRepository.findById(docId)).thenReturn(Optional.of(document));

        // Act
        var response = documentService.getDocument(docId);

        // Assert
        assertNotNull(response);
        assertEquals(docId, response.getId());
        assertEquals("test.pdf", response.getFilename());
    }

    @Test
    void testGetDocument_NonExistingId_ThrowsException() {
        // Arrange
        String docId = "non-existing";
        when(documentRepository.findById(docId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(DocumentNotFoundException.class, () -> documentService.getDocument(docId));
    }
}
