package com.example.ai.service;

import com.example.ai.dto.RetrievalResult;
import java.util.List;

public interface RetrievalService {
    
    /**
     * Search for similar content
     * @param query Query text
     * @return List of retrieval results
     */
    List<RetrievalResult> search(String query);
    
    /**
     * Search for similar content with custom parameters
     * @param query Query text
     * @param topK Number of results to return
     * @param threshold Similarity threshold
     * @return List of retrieval results
     */
    List<RetrievalResult> search(String query, int topK, double threshold);
}
