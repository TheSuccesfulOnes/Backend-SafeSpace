package com.experimentos.backend.ai.domain;

import java.time.Instant;

public class AiMessage {
    private Long id;

    private AiConversation conversation;

    private MessageSender sender;

    private String content;

    private Instant createdAt;

    protected AiMessage() {}

    public AiMessage(AiConversation conversation, MessageSender sender, String content) {
        this.conversation = conversation;
        this.sender = sender;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public AiConversation getConversation() {
        return conversation;
    }

    public MessageSender getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
