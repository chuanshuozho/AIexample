package com.example.ai.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userMessage;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String assistantReply;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
    }

    // Constructors
    public ChatMessage() {}

    public ChatMessage(String userMessage, String assistantReply) {
        this.userMessage = userMessage;
        this.assistantReply = assistantReply;
    }

    public ChatMessage(String conversationId, String userMessage, String assistantReply) {
        this.conversationId = conversationId;
        this.userMessage = userMessage;
        this.assistantReply = assistantReply;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getAssistantReply() {
        return assistantReply;
    }

    public void setAssistantReply(String assistantReply) {
        this.assistantReply = assistantReply;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
}
