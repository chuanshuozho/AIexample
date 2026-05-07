package com.example.ai.dto;

public class RetrievalResult {
    private Long chunkId;
    private String content;
    private double similarityScore;
    private Long documentId;
    private String documentName;
    private int chunkIndex;
    
    public RetrievalResult() {}
    
    public RetrievalResult(Long chunkId, String content, double similarityScore, 
                          Long documentId, String documentName, int chunkIndex) {
        this.chunkId = chunkId;
        this.content = content;
        this.similarityScore = similarityScore;
        this.documentId = documentId;
        this.documentName = documentName;
        this.chunkIndex = chunkIndex;
    }
    
    // Getters and Setters
    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
    
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
}
