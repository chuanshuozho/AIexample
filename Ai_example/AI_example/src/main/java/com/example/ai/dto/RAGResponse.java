package com.example.ai.dto;

import java.util.List;

public class RAGResponse {
    private String reply;
    private List<SourceReference> sources;
    private boolean fromKnowledgeBase;
    
    public RAGResponse() {}
    
    public RAGResponse(String reply, List<SourceReference> sources, boolean fromKnowledgeBase) {
        this.reply = reply;
        this.sources = sources;
        this.fromKnowledgeBase = fromKnowledgeBase;
    }
    
    // Getters and Setters
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    
    public List<SourceReference> getSources() { return sources; }
    public void setSources(List<SourceReference> sources) { this.sources = sources; }
    
    public boolean isFromKnowledgeBase() { return fromKnowledgeBase; }
    public void setFromKnowledgeBase(boolean fromKnowledgeBase) { this.fromKnowledgeBase = fromKnowledgeBase; }
}
