package com.example.ai.service;

import com.example.ai.entity.DocumentChunk;
import com.example.ai.entity.KnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface DocumentService {
    
    /**
     * Upload a document
     * @param file File to upload
     * @return Document metadata
     */
    KnowledgeDocument uploadDocument(MultipartFile file);
    
    /**
     * Get all documents
     * @return List of documents
     */
    List<KnowledgeDocument> getAllDocuments();
    
    /**
     * Get document by ID
     * @param documentId Document ID
     * @return Document if found
     */
    Optional<KnowledgeDocument> getDocument(Long documentId);
    
    /**
     * Update document
     * @param document Document to update
     */
    void updateDocument(KnowledgeDocument document);
    
    /**
     * Delete document and all its chunks
     * @param documentId Document ID
     */
    void deleteDocument(Long documentId);
    
    /**
     * Get all chunks for a document
     * @param documentId Document ID
     * @return List of chunks
     */
    List<DocumentChunk> getDocumentChunks(Long documentId);
    
    /**
     * Process document (chunk and embed)
     * @param documentId Document ID
     */
    void processDocument(Long documentId);
}
