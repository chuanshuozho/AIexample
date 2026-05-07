package com.example.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_summaries")
public class ConversationSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private Long sessionId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "last_message_id")
    private Long lastMessageId;

    @Column(name = "original_token_count")
    private Integer originalTokenCount;

    @Column(name = "summary_token_count")
    private Integer summaryTokenCount;

    @Column(nullable = false)
    private LocalDateTime createTime;

    @Column(nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    // Constructors
    public ConversationSummary() {}

    public ConversationSummary(Long sessionId, String summary) {
        this.sessionId = sessionId;
        this.summary = summary;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Long getLastMessageId() {
        return lastMessageId;
    }

    public void setLastMessageId(Long lastMessageId) {
        this.lastMessageId = lastMessageId;
    }

    public Integer getOriginalTokenCount() {
        return originalTokenCount;
    }

    public void setOriginalTokenCount(Integer originalTokenCount) {
        this.originalTokenCount = originalTokenCount;
    }

    public Integer getSummaryTokenCount() {
        return summaryTokenCount;
    }

    public void setSummaryTokenCount(Integer summaryTokenCount) {
        this.summaryTokenCount = summaryTokenCount;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
