package com.example.ai.service.impl;

import com.example.ai.dto.RetrievalResult;
import com.example.ai.entity.DocumentChunk;
import com.example.ai.entity.KnowledgeDocument;
import com.example.ai.repository.DocumentChunkRepository;
import com.example.ai.repository.KnowledgeDocumentRepository;
import com.example.ai.service.RetrievalService;
import com.example.ai.vectorstore.MySQLVectorStore;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RetrievalServiceImpl implements RetrievalService {
    
    private final MySQLVectorStore vectorStore;
    private final DocumentChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;
    
    @Value("${app.knowledge.retrieval.top-k:3}")
    private int defaultTopK;
    
    @Value("${app.knowledge.retrieval.similarity-threshold:0.7}")
    private double defaultThreshold;
    
    @Autowired
    public RetrievalServiceImpl(MySQLVectorStore vectorStore,
                                DocumentChunkRepository chunkRepository,
                                KnowledgeDocumentRepository documentRepository) {
        this.vectorStore = vectorStore;
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
    }
    
    @Override
    public List<RetrievalResult> search(String query) {
        return search(query, defaultTopK, defaultThreshold);
    }
    
    @Override
    public List<RetrievalResult> search(String query, int topK, double threshold) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }
        
        // Build search request
        SearchRequest request = SearchRequest.query(query)
                .withTopK(topK)
                .withSimilarityThreshold(threshold);
        
        // Execute search
        List<Document> documents = vectorStore.similaritySearch(request);
        
        // Convert to retrieval results
        List<RetrievalResult> results = new ArrayList<>();
        for (Document doc : documents) {
            Long chunkId = doc.getMetadata().get("chunkId") != null 
                    ? Long.parseLong(doc.getMetadata().get("chunkId").toString()) 
                    : null;
            Long documentId = doc.getMetadata().get("documentId") != null 
                    ? Long.parseLong(doc.getMetadata().get("documentId").toString()) 
                    : null;
            Integer chunkIndex = doc.getMetadata().get("chunkIndex") != null 
                    ? Integer.parseInt(doc.getMetadata().get("chunkIndex").toString()) 
                    : 0;
            Double similarityScore = doc.getMetadata().get("similarityScore") != null 
                    ? Double.parseDouble(doc.getMetadata().get("similarityScore").toString()) 
                    : 0.0;
            
            // Get document name
            String documentName = "Unknown";
            if (documentId != null) {
                Optional<KnowledgeDocument> docOpt = documentRepository.findById(documentId);
                if (docOpt.isPresent()) {
                    documentName = docOpt.get().getFileName();
                }
            }
            
            RetrievalResult result = new RetrievalResult(
                    chunkId,
                    doc.getContent(),
                    similarityScore,
                    documentId,
                    documentName,
                    chunkIndex
            );
            
            results.add(result);
        }
        
        return results;
    }
}
