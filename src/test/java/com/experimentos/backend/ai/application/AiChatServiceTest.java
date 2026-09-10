package com.experimentos.backend.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.experimentos.backend.shared.security.Role;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit tests for AI provider orchestration and message safety limits. */
@ExtendWith(MockitoExtension.class)
class AiChatServiceTest {

    @Mock AiConversationRepository conversations;
    @Mock AiMessageRepository messages;
    @Mock UserRepository users;
    @Mock AiAssistantProvider assistantProvider;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendsOnlyRecentHistoryToConfiguredProvider() {
        authenticateAsEmployee();
        User user = new User("maria", "maria@example.com", "hash", "Maria", Role.EMPLOYEE);
        AiConversation conversation = new AiConversation(user);
        AiMessage previousUser =
                new AiMessage(conversation, MessageSender.USER, "Mensaje anterior");
        AiMessage previousAssistant =
                new AiMessage(conversation, MessageSender.ASSISTANT, "Respuesta anterior");
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("maria", "maria"))
                .thenReturn(Optional.of(user));
        when(conversations.findByIdAndUserId(isNull(), isNull()))
                .thenReturn(Optional.of(conversation));
        when(messages.findByConversationIdOrderByIdDesc(null, Pageable.ofSize(2)))
                .thenReturn(List.of(previousAssistant, previousUser));
        when(messages.save(any(AiMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assistantProvider.isAvailable()).thenReturn(true);
        when(assistantProvider.generateReply(any(AiGenerationRequest.class)))
                .thenReturn("Respuesta de Gemini");

        AiChatService service = serviceWithHistoryLimit(2);
        service.messages(null, new AiDtos.SendMessageRequest("Necesito apoyo", "es"));

        ArgumentCaptor<AiGenerationRequest> requestCaptor =
                ArgumentCaptor.forClass(AiGenerationRequest.class);
        verify(assistantProvider).generateReply(requestCaptor.capture());
        AiGenerationRequest request = requestCaptor.getValue();

        assertThat(request.userMessage()).isEqualTo("Necesito apoyo");
        assertThat(request.language()).isEqualTo("es");
        assertThat(request.history())
                .extracting(AiConversationTurn::content)
                .containsExactly("Mensaje anterior", "Respuesta anterior");
    }

    @Test
    void usesSafeLocalFallbackWhenProviderIsNotConfigured() {
        authenticateAsEmployee();
        User user = new User("maria", "maria@example.com", "hash", "Maria", Role.EMPLOYEE);
        AiConversation conversation = new AiConversation(user);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("maria", "maria"))
                .thenReturn(Optional.of(user));
        when(conversations.findByIdAndUserId(isNull(), isNull()))
                .thenReturn(Optional.of(conversation));
        when(messages.findByConversationIdOrderByIdDesc(null, Pageable.ofSize(12)))
                .thenReturn(List.of());
        when(messages.save(any(AiMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assistantProvider.isAvailable()).thenReturn(false);

        AiChatService service = serviceWithHistoryLimit(12);
        List<AiDtos.MessageResponse> response =
                service.messages(null, new AiDtos.SendMessageRequest("Hello", "en"));

        assertThat(response).hasSize(2);
        assertThat(response.get(1).content()).contains("Thank you for sharing");
        verify(assistantProvider, never()).generateReply(any());
    }

    @Test
    void rejectsMessagesOverConfiguredLimitBeforePersistence() {
        authenticateAsEmployee();
        User user = new User("maria", "maria@example.com", "hash", "Maria", Role.EMPLOYEE);
        AiConversation conversation = new AiConversation(user);
        when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("maria", "maria"))
                .thenReturn(Optional.of(user));
        when(conversations.findByIdAndUserId(isNull(), isNull()))
                .thenReturn(Optional.of(conversation));

        AiChatService service = serviceWithHistoryLimit(12, 5);

        assertThatThrownBy(
                        () -> service.messages(null, new AiDtos.SendMessageRequest("123456", "es")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Message exceeds the maximum allowed length");
        verify(messages, never()).save(any(AiMessage.class));
    }

    private AiChatService serviceWithHistoryLimit(int historyLimit) {
        return serviceWithHistoryLimit(historyLimit, 4000);
    }

    private AiChatService serviceWithHistoryLimit(int historyLimit, int inputLimit) {
        AiProperties properties =
                new AiProperties(
                        true,
                        "secret",
                        "model",
                        "https://example.test",
                        inputLimit,
                        500,
                        4000,
                        historyLimit);
        return new AiChatService(conversations, messages, users, assistantProvider, properties);
    }

    private void authenticateAsEmployee() {
        SecurityContextHolder.getContext()
                .setAuthentication(
                        UsernamePasswordAuthenticationToken.authenticated(
                                "maria", null, List.of(() -> "ROLE_EMPLOYEE")));
    }
}
