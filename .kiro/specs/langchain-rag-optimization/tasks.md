# Implementation Plan: LangChain and RAG Optimization

## Overview

This implementation plan covers the addition of LangChain and RAG (Retrieval-Augmented Generation) capabilities to the existing AI chat application. The implementation requires upgrading from Spring Boot 2.7.18 + Java 8 to Spring Boot 3.2.0+ + Java 17 to support Spring AI framework.

## Tasks

- [x] 1. Upgrade Spring Boot and Java version
  - Update pom.xml parent version to Spring Boot 3.2.0
  - Change Java version from 1.8 to 17
  - Update MySQL connector artifact from mysql-connector-java to mysql-connector-j
  - _Requirements: 7.5_

- [x] 2. Migrate to Jakarta EE namespace
  - Replace all javax.* imports with jakarta.* in existing entities
  - Update ChatMessage entity to use jakarta.persistence.*
  - Update ConversationSession entity to use jakarta.persistence.*
  - _Requirements: 7.5_

- [x] 3. Add Spring AI dependencies
  - Add Spring AI BOM to dependencyManagement
  - Add spring-ai-deepseek dependency
  - Add document processing dependencies (Apache PDFBox, Apache POI)
  - Add JQWik dependency for property-based testing
  - _Requirements: 7.1, 7.6_

- [x] 4. Update application configuration
  - Add spring.ai.deepseek configuration section
  - Add app.knowledge configuration for chunk size, overlap, and retrieval settings
  - Configure multipart max-file-size to 10MB
  - _Requirements: 2.2, 3.2, 4.3, 4.4_

- [x] 5. Create database schema for knowledge base
  - Create knowledge_documents table
  - Create document_chunks table
  - Create vector_embeddings table
  - _Requirements: 8.1_

- [-] 6. Implement knowledge base entities
  - [x] 6.1 Create KnowledgeDocument entity
    - Define entity with all required fields (fileName, filePath, fileSize, fileType, status, chunkCount, uploadTime, processTime, errorMessage)
    - _Requirements: 2.5_

  - [x] 6.2 Create DocumentChunk entity
    - Define entity with documentId, chunkIndex, content, tokenCount, createTime
    - _Requirements: 3.6_

  - [ ] 6.3 Create VectorEmbedding entity
    - Define entity with chunkId, embedding (JSON), dimensions, createTime
    - _Requirements: 3.6, 8.2_

- [-] 7. Implement repositories
  - [ ] 7.1 Create KnowledgeDocumentRepository
    - Define CRUD operations and custom queries
    - _Requirements: 2.1, 2.4_

  - [ ] 7.2 Create DocumentChunkRepository
    - Define queries for finding chunks by documentId
    - _Requirements: 3.6_

  - [ ] 7.3 Create VectorEmbeddingRepository
    - Define queries for vector operations
    - _Requirements: 8.4_

- [-] 8. Implement ChatMemoryService
  - [ ] 8.1 Implement ChatMemoryService interface and implementation
    - Implement getHistory(sessionId) to load conversation history
    - Implement getHistoryWithTokenLimit(sessionId, maxTokens) for token truncation
    - Implement saveMessage(sessionId, userMessage, assistantReply)
    - Implement clearHistory(sessionId)
    - Implement countTokens(text) using TikToken or approximation
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [ ] 8.2 Write property test for token truncation
    - **Property 2: 历史消息 Token 截断**
    - **Validates: Requirements 1.2**

  - [ ] 8.3 Write property test for session history loading
    - **Property 1: 会话历史加载**
    - **Validates: Requirements 1.1**

  - [ ] 8.4 Write property test for session switching
    - **Property 3: 会话切换历史加载**
    - **Validates: Requirements 1.4**

- [-] 9. Implement EmbeddingService
  - [ ] 9.1 Implement EmbeddingService interface and implementation
    - Implement embed(text) for single text embedding
    - Implement embedBatch(texts) for batch embedding
    - Implement getDimensions() for vector dimension
    - Integrate with Spring AI EmbeddingModel
    - Implement embedding cache to avoid redundant computation
    - _Requirements: 9.1, 9.2, 9.3, 9.5_

  - [ ] 9.2 Write property test for embedding cache
    - **Property 22: 向量嵌入缓存**
    - **Validates: Requirements 9.5**

  - [ ] 9.3 Write unit tests for error handling
    - Test Embedding API failure scenarios
    - _Requirements: 9.4_

- [-] 10. Implement MySQLVectorStore
  - [ ] 10.1 Implement MySQLVectorStore class
    - Implement VectorStore interface from Spring AI
    - Implement add(List<Document>) for storing vectors
    - Implement delete(List<String>) for removing vectors
    - Implement similaritySearch(SearchRequest) for retrieval
    - Implement deleteByDocumentId(Long) for batch deletion
    - Implement cosineSimilarity(float[], float[]) for similarity calculation
    - _Requirements: 8.2, 8.3, 8.4, 8.5_

  - [ ] 10.2 Write property test for cosine similarity
    - **Property 18: 余弦相似度计算**
    - **Validates: Requirements 8.3**

  - [ ] 10.3 Write property test for vector CRUD operations
    - **Property 19: 向量 CRUD 操作**
    - **Validates: Requirements 8.4**

  - [ ] 10.4 Write property test for batch deletion by document ID
    - **Property 20: 按文档 ID 批量删除向量**
    - **Validates: Requirements 8.5**

- [-] 11. Implement DocumentService
  - [ ] 11.1 Implement DocumentService interface and implementation
    - Implement uploadDocument(MultipartFile) with file validation
    - Implement getAllDocuments() for listing documents
    - Implement getDocument(Long) for document details
    - Implement deleteDocument(Long) with cascade deletion
    - Implement getDocumentChunks(Long) for viewing chunks
    - Implement processDocument(Long) for chunking and embedding
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 3.1, 3.5_

  - [ ] 11.2 Implement document content extraction
    - Implement PDF extraction using Apache PDFBox
    - Implement TXT/MD direct reading
    - Implement DOCX extraction using Apache POI
    - _Requirements: 2.3_

  - [ ] 11.3 Implement document chunking with TokenTextSplitter
    - Configure chunk size (default 500) and overlap (default 50)
    - Split document content into chunks
    - _Requirements: 3.1, 3.2_

  - [ ] 11.4 Write property test for document chunking
    - **Property 6: 文档分片**
    - **Validates: Requirements 3.1, 3.2**

  - [ ] 11.5 Write property test for document metadata integrity
    - **Property 5: 文档元数据完整性**
    - **Validates: Requirements 2.5**

  - [ ] 11.6 Write property test for vectorization failure handling
    - **Property 7: 向量化失败处理**
    - **Validates: Requirements 3.5**

  - [ ] 11.7 Write property test for vector storage integrity
    - **Property 8: 向量存储完整性**
    - **Validates: Requirements 3.6**

- [-] 12. Implement RetrievalService
  - [ ] 12.1 Implement RetrievalService interface and implementation
    - Implement search(query) with default parameters
    - Implement search(query, topK, threshold) with custom parameters
    - Integrate with EmbeddingService and MySQLVectorStore
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_

  - [ ] 12.2 Write property test for Top-K retrieval
    - **Property 9: 相似度搜索 Top-K**
    - **Validates: Requirements 4.2, 4.3**

  - [ ] 12.3 Write property test for similarity threshold filtering
    - **Property 10: 相似度阈值过滤**
    - **Validates: Requirements 4.4**

  - [ ] 12.4 Write property test for empty result handling
    - **Property 11: 空结果处理**
    - **Validates: Requirements 4.5**

- [-] 13. Implement RAGService
  - [ ] 13.1 Implement RAGService interface and implementation
    - Implement chatWithRAG(sessionId, question) for RAG-enhanced chat
    - Implement buildPrompt(systemPrompt, context, history, question)
    - Integrate ChatMemoryService, RetrievalService, and ChatClient
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_

  - [ ] 13.2 Write property test for RAG retrieval priority
    - **Property 12: RAG 检索优先**
    - **Validates: Requirements 5.1**

  - [ ] 13.3 Write property test for context injection
    - **Property 13: 上下文注入**
    - **Validates: Requirements 5.2**

  - [ ] 13.4 Write property test for prompt completeness
    - **Property 14: 提示词完整性**
    - **Validates: Requirements 5.3**

  - [ ] 13.5 Write property test for no-retrieval fallback
    - **Property 15: 无检索结果降级**
    - **Validates: Requirements 5.4**

  - [ ] 13.6 Write property test for source attribution
    - **Property 16: 来源标注**
    - **Validates: Requirements 5.5**

- [ ] 14. Checkpoint - Ensure all service tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [-] 15. Implement REST API controllers
  - [ ] 15.1 Implement KnowledgeController
    - Implement POST /api/knowledge/documents for document upload
    - Implement GET /api/knowledge/documents for document list
    - Implement GET /api/knowledge/documents/{id} for document details
    - Implement DELETE /api/knowledge/documents/{id} for document deletion
    - Implement GET /api/knowledge/documents/{id}/chunks for chunk viewing
    - _Requirements: 10.1, 10.2, 10.3, 10.5_

  - [ ] 15.2 Implement RAGController
    - Implement POST /api/chat/rag for RAG-enhanced chat
    - _Requirements: 10.4_

  - [ ] 15.3 Implement global exception handler
    - Create standardized error response format
    - Handle custom exceptions (DocumentTooLargeException, UnsupportedFileTypeException, etc.)
    - _Requirements: 10.6_

  - [ ] 15.4 Write property test for API error response format
    - **Property 23: API 错误响应格式**
    - **Validates: Requirements 10.6**

- [x] 16. Implement frontend knowledge base management UI
  - [ ] 16.1 Create knowledge.html page
    - Add document list display area
    - Add file upload area with drag-and-drop support
    - Add upload progress indicator
    - _Requirements: 6.1, 6.5_

  - [ ] 16.2 Implement document list display
    - Show file name, size, upload time, chunk count, and status
    - Add delete button with confirmation dialog
    - _Requirements: 6.2, 6.3_

  - [ ] 16.3 Implement document upload functionality
    - Support drag-and-drop file upload
    - Show upload progress
    - Handle upload success and error states
    - _Requirements: 6.5_

  - [ ] 16.4 Implement document deletion functionality
    - Show confirmation dialog before deletion
    - Call DELETE API and refresh list
    - _Requirements: 6.3, 6.4_

  - [ ] 16.5 Add knowledge base navigation to main chat page
    - Add link to knowledge base page in index.html
    - _Requirements: 6.1_

- [ ] 17. Checkpoint - Ensure all integration tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [ ] 18. Write integration tests
  - [ ] 18.1 Write database integration tests
    - Test vector store CRUD operations with Testcontainers MySQL
    - Test document and chunk cascade deletion
    - _Requirements: 8.4, 8.5_

  - [ ] 18.2 Write API integration tests
    - Test document upload flow
    - Test RAG chat flow
    - Test error handling
    - _Requirements: 2.1, 5.1, 10.6_

  - [ ] 18.3 Write property test for document cascade deletion
    - **Property 17: 文档级联删除**
    - **Validates: Requirements 6.4_

- [ ] 19. Final checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- The implementation uses Java 17 and Spring Boot 3.2.0+ as required by Spring AI
- MySQL is used for vector storage to minimize infrastructure complexity
- DeepSeek API is used for both chat and embedding services
