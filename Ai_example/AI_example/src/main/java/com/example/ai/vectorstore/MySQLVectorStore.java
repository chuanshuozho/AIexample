package com.example.ai.vectorstore;

import com.example.ai.entity.DocumentChunk;
import com.example.ai.entity.VectorEmbedding;
import com.example.ai.repository.DocumentChunkRepository;
import com.example.ai.repository.VectorEmbeddingRepository;
import com.example.ai.service.EmbeddingService;
import com.example.ai.service.impl.DocumentServiceImpl;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class MySQLVectorStore implements VectorStore {
    
    private final VectorEmbeddingRepository vectorEmbeddingRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final EmbeddingService embeddingService;
    
    @Value("${app.knowledge.retrieval.top-k:3}")
    private int defaultTopK;
    
    @Value("${app.knowledge.retrieval.similarity-threshold:0.7}")
    private double defaultThreshold;
    
    @Autowired
    public MySQLVectorStore(VectorEmbeddingRepository vectorEmbeddingRepository,
                            DocumentChunkRepository documentChunkRepository,
                            EmbeddingService embeddingService) {
        this.vectorEmbeddingRepository = vectorEmbeddingRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.embeddingService = embeddingService;
    }
    
    @Override
    @Transactional
    public void add(List<Document> documents) {
        for (Document doc : documents) {
            Long chunkId = doc.getMetadata().get("chunkId") != null 
                    ? Long.parseLong(doc.getMetadata().get("chunkId").toString()) 
                    : null;
            
            if (chunkId == null) {
                continue;
            }
            
            float[] embedding = doc.getEmbedding();
            byte[] embeddingBytes = floatArrayToBytes(embedding);
            
            VectorEmbedding vectorEmbedding = new VectorEmbedding(chunkId, embeddingBytes, embedding.length);
            vectorEmbeddingRepository.save(vectorEmbedding);
        }
    }
    
    @Override
    @Transactional
    public Optional<Boolean> delete(List<String> idList) {
        boolean deleted = false;
        for (String id : idList) {
            try {
                Long chunkId = Long.parseLong(id);
                vectorEmbeddingRepository.deleteByChunkId(chunkId);
                deleted = true;
            } catch (NumberFormatException e) {
                // Skip invalid IDs
            }
        }
        return Optional.of(deleted);
    }
    
    @Override
    public List<Document> similaritySearch(SearchRequest request) {
        String query = request.getQuery();
        int topK = request.getTopK() > 0 ? request.getTopK() : defaultTopK;
        double threshold = request.getSimilarityThreshold() > 0 ? request.getSimilarityThreshold() : defaultThreshold;
        
        // Generate embedding for query
        float[] queryEmbedding = embeddingService.embed(query);
        
        // Use pagination to avoid loading all embeddings into memory at once
        List<SimilarityResult> results = new ArrayList<>();
        
        int pageSize = 100; // 减小分页大小，降低内存占用
        int page = 0;
        boolean hasMoreData = true;
        
        while (hasMoreData) {
            // Fetch embeddings in pages
            org.springframework.data.domain.Pageable pageable = 
                org.springframework.data.domain.PageRequest.of(page, pageSize);
            org.springframework.data.domain.Page<VectorEmbedding> embeddingPage = 
                vectorEmbeddingRepository.findAll(pageable);
            
            if (!embeddingPage.hasContent()) {
                hasMoreData = false;
                break;
            }
            
            // Calculate similarity for current batch
            for (VectorEmbedding ve : embeddingPage.getContent()) {
                try {
                    // 使用二进制格式解析
                    float[] storedEmbedding = bytesToFloatArray(ve.getEmbedding());
                    double similarity = cosineSimilarity(queryEmbedding, storedEmbedding);
                    
                    if (similarity >= threshold) {
                        results.add(new SimilarityResult(ve.getChunkId(), similarity));
                    }
                } catch (Exception e) {
                    // Skip invalid embeddings
                    System.err.println("Error processing embedding for chunk " + ve.getChunkId() + ": " + e.getMessage());
                }
            }
            
            // Move to next page or stop if we have enough results
            page++;
            hasMoreData = embeddingPage.hasNext();
            
            // Early termination if we already have enough high-quality results
            if (results.size() >= topK * 3) {
                break;
            }
        }
        
        // Sort by similarity (descending) and limit to topK
        results.sort((a, b) -> Double.compare(b.similarity, a.similarity));
        results = results.stream().limit(topK).collect(Collectors.toList());
        
        // Build document results
        List<Document> documents = new ArrayList<>();
        for (SimilarityResult result : results) {
            Optional<DocumentChunk> chunkOpt = documentChunkRepository.findById(result.chunkId);
            if (chunkOpt.isPresent()) {
                DocumentChunk chunk = chunkOpt.get();
                Document doc = new Document(chunk.getContent());
                doc.getMetadata().put("chunkId", chunk.getId());
                doc.getMetadata().put("documentId", chunk.getDocumentId());
                doc.getMetadata().put("chunkIndex", chunk.getChunkIndex());
                doc.getMetadata().put("similarityScore", result.similarity);
                documents.add(doc);
            }
        }
        
        return documents;
    }
    
    @Transactional
    public void deleteByDocumentId(Long documentId) {
        vectorEmbeddingRepository.deleteByDocumentId(documentId);
    }
    
    public double cosineSimilarity(float[] vector1, float[] vector2) {
        if (vector1.length != vector2.length) {
            throw new IllegalArgumentException("Vectors must have the same dimension");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.length; i++) {
            dotProduct += vector1[i] * vector2[i];
            norm1 += vector1[i] * vector1[i];
            norm2 += vector2[i] * vector2[i];
        }
        
        if (norm1 == 0 || norm2 == 0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    /**
     * 将 float 数组转换为字节数组
     */
    private byte[] floatArrayToBytes(float[] floats) {
        ByteBuffer buffer = ByteBuffer.allocate(floats.length * 4);
        for (float f : floats) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }
    
    /**
     * 将字节数组转换为 float 数组
     */
    private float[] bytesToFloatArray(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        float[] floats = new float[bytes.length / 4];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = buffer.getFloat();
        }
        return floats;
    }
    
    private static class SimilarityResult {
        final Long chunkId;
        final double similarity;
        
        SimilarityResult(Long chunkId, double similarity) {
            this.chunkId = chunkId;
            this.similarity = similarity;
        }
    }
}
