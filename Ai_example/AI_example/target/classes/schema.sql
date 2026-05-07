-- Knowledge Base Tables for RAG functionality

-- Knowledge Documents Table
CREATE TABLE IF NOT EXISTS knowledge_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    chunk_count INT DEFAULT 0,
    upload_time DATETIME NOT NULL,
    process_time DATETIME,
    error_message TEXT,
    INDEX idx_status (status),
    INDEX idx_upload_time (upload_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Document Chunks Table
CREATE TABLE IF NOT EXISTS document_chunks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    token_count INT,
    create_time DATETIME NOT NULL,
    INDEX idx_document_id (document_id),
    FOREIGN KEY (document_id) REFERENCES knowledge_documents(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Vector Embeddings Table
CREATE TABLE IF NOT EXISTS vector_embeddings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    chunk_id BIGINT NOT NULL UNIQUE,
    embedding JSON NOT NULL,
    dimensions INT NOT NULL,
    create_time DATETIME NOT NULL,
    INDEX idx_chunk_id (chunk_id),
    FOREIGN KEY (chunk_id) REFERENCES document_chunks(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
