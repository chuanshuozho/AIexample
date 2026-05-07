package com.example.ai.repository;

import com.example.ai.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for KnowledgeDocument entity.
 * Provides CRUD operations and custom queries for document management.
 * 
 * Requirements: 2.1, 2.4
 */
@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    
    // ========== Status-based queries ==========
    
    /**
     * Find all documents by status.
     * @param status the document status (PENDING, PROCESSING, COMPLETED, FAILED)
     * @return list of documents with the specified status
     */
    List<KnowledgeDocument> findByStatus(String status);
    
    /**
     * Find all documents by status, ordered by upload time (newest first).
     * @param status the document status
     * @return list of documents with the specified status
     */
    List<KnowledgeDocument> findByStatusOrderByUploadTimeDesc(String status);
    
    /**
     * Count documents by status.
     * @param status the document status
     * @return count of documents with the specified status
     */
    long countByStatus(String status);
    
    /**
     * Check if any document exists with the given status.
     * @param status the document status
     * @return true if at least one document exists with the status
     */
    boolean existsByStatus(String status);
    
    // ========== File type-based queries ==========
    
    /**
     * Find all documents by file type.
     * @param fileType the file type (PDF, TXT, MD, DOCX)
     * @return list of documents with the specified file type
     */
    List<KnowledgeDocument> findByFileType(String fileType);
    
    /**
     * Find all documents by file type, ordered by upload time (newest first).
     * @param fileType the file type
     * @return list of documents with the specified file type
     */
    List<KnowledgeDocument> findByFileTypeOrderByUploadTimeDesc(String fileType);
    
    /**
     * Count documents by file type.
     * @param fileType the file type
     * @return count of documents with the specified file type
     */
    long countByFileType(String fileType);
    
    // ========== Ordering queries ==========
    
    /**
     * Find all documents ordered by upload time (newest first).
     * @return list of all documents
     */
    List<KnowledgeDocument> findAllByOrderByUploadTimeDesc();
    
    /**
     * Find all documents ordered by upload time (oldest first).
     * @return list of all documents
     */
    List<KnowledgeDocument> findAllByOrderByUploadTimeAsc();
    
    // ========== File name queries ==========
    
    /**
     * Find documents by file name (exact match).
     * @param fileName the file name
     * @return list of documents with the specified file name
     */
    List<KnowledgeDocument> findByFileName(String fileName);
    
    /**
     * Find documents by file name containing the given string (case-insensitive).
     * @param fileName the file name pattern to search for
     * @return list of matching documents
     */
    List<KnowledgeDocument> findByFileNameContainingIgnoreCase(String fileName);
    
    /**
     * Check if a document with the given file name exists.
     * @param fileName the file name
     * @return true if a document with the file name exists
     */
    boolean existsByFileName(String fileName);
    
    // ========== Combined queries ==========
    
    /**
     * Find documents by status and file type.
     * @param status the document status
     * @param fileType the file type
     * @return list of matching documents
     */
    List<KnowledgeDocument> findByStatusAndFileType(String status, String fileType);
    
    /**
     * Find documents by status and file type, ordered by upload time (newest first).
     * @param status the document status
     * @param fileType the file type
     * @return list of matching documents
     */
    List<KnowledgeDocument> findByStatusAndFileTypeOrderByUploadTimeDesc(String status, String fileType);
    
    // ========== Custom JPQL queries ==========
    
    /**
     * Find all completed documents that have chunks.
     * @return list of completed documents with chunk count > 0
     */
    @Query("SELECT kd FROM KnowledgeDocument kd WHERE kd.status = 'COMPLETED' AND kd.chunkCount > 0")
    List<KnowledgeDocument> findCompletedDocumentsWithChunks();
    
    /**
     * Find all failed documents with error messages.
     * @return list of failed documents
     */
    @Query("SELECT kd FROM KnowledgeDocument kd WHERE kd.status = 'FAILED' AND kd.errorMessage IS NOT NULL")
    List<KnowledgeDocument> findFailedDocumentsWithErrors();
    
    /**
     * Find document by ID with status check.
     * @param id the document ID
     * @param status the expected status
     * @return the document if found with the expected status
     */
    Optional<KnowledgeDocument> findByIdAndStatus(Long id, String status);
    
    /**
     * Count total chunks across all documents.
     * @return total chunk count
     */
    @Query("SELECT COALESCE(SUM(kd.chunkCount), 0) FROM KnowledgeDocument kd")
    long countTotalChunks();
    
    /**
     * Count total chunks for documents with a specific status.
     * @param status the document status
     * @return total chunk count for the status
     */
    @Query("SELECT COALESCE(SUM(kd.chunkCount), 0) FROM KnowledgeDocument kd WHERE kd.status = :status")
    long countTotalChunksByStatus(@Param("status") String status);
    
    /**
     * Find documents with file size greater than the specified value.
     * @param fileSize the minimum file size
     * @return list of documents larger than the specified size
     */
    List<KnowledgeDocument> findByFileSizeGreaterThan(Long fileSize);
    
    /**
     * Find documents with file size between two values.
     * @param minSize minimum file size
     * @param maxSize maximum file size
     * @return list of documents within the size range
     */
    List<KnowledgeDocument> findByFileSizeBetween(Long minSize, Long maxSize);
}
