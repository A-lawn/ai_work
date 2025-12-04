package com.rag.ops.document.controller;

import com.rag.ops.document.dto.DocumentResponse;
import com.rag.ops.document.dto.DocumentUploadResponse;
import com.rag.ops.document.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @Test
    void testUploadDocument_ValidFile_ReturnsOk() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        DocumentUploadResponse response = DocumentUploadResponse.builder()
                .documentId("doc-123")
                .filename("test.pdf")
                .status("PROCESSING")
                .message("文档上传成功")
                .build();

        when(documentService.uploadDocument(any())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(multipart("/api/v1/documents/upload")
                        .file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value("doc-123"))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void testGetDocument_ExistingId_ReturnsDocument() throws Exception {
        // Arrange
        String docId = "doc-123";
        DocumentResponse response = DocumentResponse.builder()
                .id(docId)
                .filename("test.pdf")
                .status("COMPLETED")
                .build();

        when(documentService.getDocument(docId)).thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/v1/documents/{id}", docId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(docId))
                .andExpect(jsonPath("$.filename").value("test.pdf"));
    }

    @Test
    void testDeleteDocument_ExistingId_ReturnsNoContent() throws Exception {
        // Arrange
        String docId = "doc-123";

        // Act & Assert
        mockMvc.perform(delete("/api/v1/documents/{id}", docId))
                .andExpect(status().isNoContent());
    }
}
