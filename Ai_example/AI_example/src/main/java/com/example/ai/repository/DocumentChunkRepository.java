package com.example.ai.repository;

import com.example.ai.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {
    
    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(Long documentId);
    
    void deleteByDocumentId(Long documentId);
    
    List<DocumentChunk> findByDocumentId(Long documentId);
}
