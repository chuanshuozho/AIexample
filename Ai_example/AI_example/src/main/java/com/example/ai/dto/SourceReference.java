package com.example.ai.dto;

public class SourceReference {
    private Long documentId;
    private String documentName;
    private String chunkContent;
    private double similarityScore;
    
    public SourceReference() {}
    
    public SourceReference(Long documentId, String documentName, String chunkContent, double similarityScore) {
        this.documentId = documentId;
        this.documentName = documentName;
        this.chunkContent = chunkContent;
        this.similarityScore = similarityScore;
    }
    
    // Getters and Setters
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    
    public String getDocumentName() { return documentName; }
    public void setDocumentName(String documentName) { this.documentName = documentName; }
    
    public String getChunkContent() { return chunkContent; }
    public void setChunkContent(String chunkContent) { this.chunkContent = chunkContent; }
    
    public double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(double similarityScore) { this.similarityScore = similarityScore; }
}
