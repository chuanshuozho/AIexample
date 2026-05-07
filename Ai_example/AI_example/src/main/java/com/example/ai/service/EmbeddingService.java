package com.example.ai.service;

import java.util.List;

public interface EmbeddingService {
    
    /**
     * Generate embedding for a single text
     * @param text Text content
     * @return Vector embedding as float array
     */
    float[] embed(String text);
    
    /**
     * Generate embeddings for multiple texts
     * @param texts List of text content
     * @return List of vector embeddings
     */
    List<float[]> embedBatch(List<String> texts);
    
    /**
     * Get the dimension of embeddings
     * @return Embedding dimension
     */
    int getDimensions();
}
