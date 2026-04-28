package com.example.ai.dto;

public class AiResponse {
    private String reply;
    private Long sessionId;

    public AiResponse() {}

    public AiResponse(String reply) {
        this.reply = reply;
    }

    public AiResponse(String reply, Long sessionId) {
        this.reply = reply;
        this.sessionId = sessionId;
    }

    public String getReply() {
        return reply;
    }

    public void setReply(String reply) {
        this.reply = reply;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
}