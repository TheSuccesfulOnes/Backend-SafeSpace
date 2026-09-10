package com.experimentos.backend.ai.domain;

/** A previous turn sent to an AI provider without exposing persistence concerns. */
public record AiConversationTurn(MessageSender sender, String content) {}
