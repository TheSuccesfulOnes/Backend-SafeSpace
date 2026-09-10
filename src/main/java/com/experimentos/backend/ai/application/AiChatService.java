package com.experimentos.backend.ai.application;

import com.experimentos.backend.ai.domain.AiAssistantProvider;
import com.experimentos.backend.ai.domain.AiConversation;
import com.experimentos.backend.ai.domain.AiConversationTurn;
import com.experimentos.backend.ai.domain.AiGenerationRequest;
import com.experimentos.backend.ai.domain.AiMessage;
import com.experimentos.backend.ai.domain.MessageSender;
import com.experimentos.backend.ai.infrastructure.AiConversationRepository;
import com.experimentos.backend.ai.infrastructure.AiMessageRepository;
import com.experimentos.backend.ai.interfaces.AiDtos;
import com.experimentos.backend.iam.domain.User;
import com.experimentos.backend.iam.infrastructure.UserRepository;
import com.experimentos.backend.shared.security.CurrentUser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Application service for employee-owned AI conversations. */
@Service
public class AiChatService {

    private final AiConversationRepository conversations;
    private final AiMessageRepository messages;
    private final UserRepository users;
    private final AiAssistantProvider assistantProvider;
    private final AiProperties aiProperties;

    public AiChatService(
            AiConversationRepository conversations,
            AiMessageRepository messages,
            UserRepository users,
            AiAssistantProvider assistantProvider,
            AiProperties aiProperties) {
        this.conversations = conversations;
        this.messages = messages;
        this.users = users;
        this.assistantProvider = assistantProvider;
        this.aiProperties = aiProperties;
    }

    @Transactional
    public AiDtos.ConversationResponse createConversation() {
        AiConversation conversation = conversations.save(new AiConversation(currentUser()));
        return new AiDtos.ConversationResponse(conversation.getId(), conversation.getTitle());
    }

    @Transactional(readOnly = true)
    public List<AiDtos.ConversationResponse> conversations() {
        return conversations.findByUserIdOrderByIdDesc(currentUser().getId()).stream()
                .map(item -> new AiDtos.ConversationResponse(item.getId(), item.getTitle()))
                .toList();
    }

    @Transactional
    public AiDtos.ConversationResponse renameConversation(
            Long conversationId, AiDtos.UpdateConversationRequest request) {
        AiConversation conversation = ownedConversation(conversationId);
        String title = request.title() == null ? "" : request.title().trim();
        if (title.isBlank()) {
            throw new IllegalArgumentException("Conversation title cannot be blank");
        }
        conversation.rename(title);
        return new AiDtos.ConversationResponse(conversation.getId(), conversation.getTitle());
    }

    @Transactional
    public void deleteConversation(Long conversationId) {
        AiConversation conversation = ownedConversation(conversationId);
        messages.deleteByConversationId(conversationId);
        messages.flush();
        conversations.delete(conversation);
        conversations.flush();
    }

    @Transactional
    public List<AiDtos.MessageResponse> messages(
            Long conversationId, AiDtos.SendMessageRequest request) {
        AiConversation conversation = ownedConversation(conversationId);
        String content = request.content().trim();
        if (content.length() > aiProperties.inputLimit()) {
            throw new IllegalArgumentException("Message exceeds the maximum allowed length");
        }
        List<AiConversationTurn> history = previousHistory(conversationId);

        AiMessage userMessage =
                messages.save(new AiMessage(conversation, MessageSender.USER, content));
        String assistantReply = generateReply(history, content, request.language());
        if (assistantReply.length() > aiProperties.outputLimit()) {
            assistantReply = assistantReply.substring(0, aiProperties.outputLimit()).trim();
        }
        AiMessage assistantMessage =
                messages.save(new AiMessage(conversation, MessageSender.ASSISTANT, assistantReply));

        return List.of(toResponse(userMessage), toResponse(assistantMessage));
    }

    @Transactional(readOnly = true)
    public List<AiDtos.MessageResponse> history(Long conversationId) {
        ownedConversation(conversationId);
        return messages.findByConversationIdOrderByIdAsc(conversationId).stream()
                .map(this::toResponse)
                .toList();
    }

    private String generateReply(
            List<AiConversationTurn> history, String content, String language) {
        if (!assistantProvider.isAvailable()) {
            return localReply(language);
        }

        return assistantProvider
                .generateReply(new AiGenerationRequest(history, content, language))
                .trim();
    }

    private List<AiConversationTurn> previousHistory(Long conversationId) {
        int limit = aiProperties.historyLimit();
        if (limit == 0) {
            return List.of();
        }

        List<AiMessage> recentMessages =
                messages.findByConversationIdOrderByIdDesc(
                        conversationId, PageRequest.of(0, limit));
        List<AiMessage> chronologicalMessages = new ArrayList<>(recentMessages);
        Collections.reverse(chronologicalMessages);

        return chronologicalMessages.stream()
                .map(message -> new AiConversationTurn(message.getSender(), message.getContent()))
                .toList();
    }

    private String localReply(String language) {
        if ("en".equalsIgnoreCase(language)) {
            return "Thank you for sharing this. I am here to listen. For personalized guidance, also consider talking with a Human Resources professional.";
        }
        return "Gracias por compartirlo. Estoy aquí para escucharte. Para una orientación personalizada, conversa también con un profesional de Recursos Humanos.";
    }

    private AiDtos.MessageResponse toResponse(AiMessage message) {
        return new AiDtos.MessageResponse(
                message.getId(),
                message.getSender().name(),
                message.getContent(),
                message.getCreatedAt());
    }

    private AiConversation ownedConversation(Long id) {
        return conversations
                .findByIdAndUserId(id, currentUser().getId())
                .orElseThrow(() -> new IllegalArgumentException("Conversation was not found"));
    }

    private User currentUser() {
        return users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                        CurrentUser.username(), CurrentUser.username())
                .orElseThrow(
                        () -> new IllegalArgumentException("Authenticated user was not found"));
    }
}
