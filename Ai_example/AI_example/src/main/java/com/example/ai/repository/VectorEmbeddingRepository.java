package com.example.ai.repository;

import com.example.ai.entity.VectorEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VectorEmbeddingRepository extends JpaRepository<VectorEmbedding, Long> {
    
    Optional<VectorEmbedding> findByChunkId(Long chunkId);
    
    void deleteByChunkId(Long chunkId);
    
    @Query("SELECT ve FROM VectorEmbedding ve WHERE ve.chunkId IN :chunkIds")
    List<VectorEmbedding> findByChunkIds(@Param("chunkIds") List<Long> chunkIds);
    
    @Query("SELECT ve.chunkId FROM VectorEmbedding ve " +
           "JOIN DocumentChunk dc ON ve.chunkId = dc.id " +
           "WHERE dc.documentId = :documentId")
    List<Long> findChunkIdsByDocumentId(@Param("documentId") Long documentId);
    
    @Modifying
    @Query(value = "DELETE ve FROM vector_embeddings ve " +
                   "INNER JOIN document_chunks dc ON ve.chunk_id = dc.id " +
                   "WHERE dc.document_id = :documentId", nativeQuery = true)
    void deleteByDocumentId(@Param("documentId") Long documentId);
}
