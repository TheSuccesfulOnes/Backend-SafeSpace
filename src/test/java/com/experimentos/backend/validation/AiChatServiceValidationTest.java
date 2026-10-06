package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.ai.application.*;
import com.experimentos.backend.ai.domain.*;
import com.experimentos.backend.ai.infrastructure.*;
import com.experimentos.backend.ai.interfaces.*;
import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.shared.security.*;
import java.util.*;

class AiChatServiceValidationTest extends ScenarioContract {
    static class Fixture {
        final AiConversationRepository conversations = mock(AiConversationRepository.class);
        final AiMessageRepository messages = mock(AiMessageRepository.class);
        final UserRepository users = mock(UserRepository.class);
        final AiAssistantProvider provider = mock(AiAssistantProvider.class);
        final User actor = user(1, Role.EMPLOYEE);
        final AiConversation conversation = id(new AiConversation(actor), 10);
        AiChatService service = build(2);

        AiChatService build(int history) {
            return new AiChatService(
                    conversations,
                    messages,
                    users,
                    provider,
                    new AiProperties(
                            false,
                            null,
                            "test-model",
                            "https://provider.invalid",
                            5,
                            128,
                            8,
                            history));
        }

        AiChatController controller() {
            return new AiChatController(service);
        }

        Fixture() {
            authenticate(actor);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(conversations.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(conversation));
            when(conversations.save(any())).thenAnswer(i -> id(i.getArgument(0), 10));
            when(messages.save(any())).thenAnswer(i -> id(i.getArgument(0), 20));
        }

        void missing() {
            when(conversations.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        }

        void available(String answer) {
            when(provider.isAvailable()).thenReturn(true);
            when(provider.generateReply(any())).thenReturn(answer);
        }

        List<AiDtos.MessageResponse> send(String message, String lang) {
            return service.messages(10L, new AiDtos.SendMessageRequest(message, lang));
        }

        void noMessage() {
            verify(messages, never()).save(any());
            verify(provider, never()).generateReply(any());
        }

        void noConversationWrite() {
            verify(conversations, never()).save(any());
        }
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "foreign conversation rename blocked",
                        () -> {
                            var f = new Fixture();
                            f.missing();
                            rejected(
                                    () ->
                                            f.service.renameConversation(
                                                    10L,
                                                    new AiDtos.UpdateConversationRequest("New")),
                                    "not found");
                            f.noConversationWrite();
                        }),
                unit(
                        "foreign conversation delete blocked",
                        () -> {
                            var f = new Fixture();
                            f.missing();
                            rejected(() -> f.service.deleteConversation(10L), "not found");
                            verify(f.messages, never()).deleteByConversationId(any());
                            verify(f.conversations, never()).delete(any());
                        }),
                unit(
                        "foreign conversation history blocked",
                        () -> {
                            var f = new Fixture();
                            f.missing();
                            rejected(() -> f.service.history(10L), "not found");
                            verifyNoInteractions(f.messages);
                        }),
                unit(
                        "foreign conversation send blocked",
                        () -> {
                            var f = new Fixture();
                            f.missing();
                            rejected(() -> f.send("Hello", "es"), "not found");
                            f.noMessage();
                        }),
                unit(
                        "null rename rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.renameConversation(
                                                    10L,
                                                    new AiDtos.UpdateConversationRequest(null)),
                                    "blank");
                            f.noConversationWrite();
                        }),
                unit(
                        "blank rename rejected",
                        () -> {
                            var f = new Fixture();
                            rejected(
                                    () ->
                                            f.service.renameConversation(
                                                    10L,
                                                    new AiDtos.UpdateConversationRequest(" \t")),
                                    "blank");
                            f.noConversationWrite();
                        }),
                unit(
                        "configured input limit enforced before save",
                        () -> {
                            var f = new Fixture();
                            rejected(() -> f.send("123456", "es"), "maximum");
                            f.noMessage();
                        }),
                unit(
                        "provider output trimmed truncated to configured bound",
                        () -> {
                            var f = new Fixture();
                            f.available("  1234567890  ");
                            var r = f.send("Hello", "es");
                            assertThat(r.get(1).content()).isEqualTo("12345678");
                            verify(f.messages)
                                    .save(
                                            argThat(
                                                    m ->
                                                            m.getSender() == MessageSender.ASSISTANT
                                                                    && m.getContent().length()
                                                                            == 8));
                        }),
                unit(
                        "zero history avoids repository page read",
                        () -> {
                            var f = new Fixture();
                            f.service = f.build(0);
                            f.send("Hello", "es");
                            verify(f.messages, never())
                                    .findByConversationIdOrderByIdDesc(anyLong(), any());
                        }),
                unit(
                        "provider failure compensates user message",
                        () -> {
                            var f = new Fixture();
                            when(f.provider.isAvailable()).thenReturn(true);
                            when(f.provider.generateReply(any()))
                                    .thenThrow(new AiProviderException("synthetic"));
                            assertThatThrownBy(() -> f.send("Hello", "es"))
                                    .isInstanceOf(AiProviderException.class);
                            verify(f.messages)
                                    .delete(argThat(m -> m.getSender() == MessageSender.USER));
                        }),
                unit(
                        "assistant write failure compensates user message",
                        () -> {
                            var f = new Fixture();
                            f.available("Support");
                            doAnswer(
                                            i -> {
                                                AiMessage m = i.getArgument(0);
                                                if (m.getSender() == MessageSender.ASSISTANT)
                                                    throw new IllegalStateException("synthetic");
                                                return id(m, 20);
                                            })
                                    .when(f.messages)
                                    .save(any());
                            assertThatThrownBy(() -> f.send("Hello", "es")).hasMessage("synthetic");
                            verify(f.messages)
                                    .delete(argThat(m -> m.getSender() == MessageSender.USER));
                        }),
                unit(
                        "missing current user blocks all access",
                        () -> {
                            var f = new Fixture();
                            when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                            "actor", "actor"))
                                    .thenReturn(Optional.empty());
                            rejected(() -> f.service.conversations(), "Authenticated user");
                            verifyNoInteractions(f.conversations, f.messages);
                        }),
                unit(
                        "history reversed to chronological bounded context",
                        () -> {
                            var f = new Fixture();
                            f.available("Support");
                            var old = new AiMessage(f.conversation, MessageSender.USER, "Old");
                            var newer =
                                    new AiMessage(f.conversation, MessageSender.ASSISTANT, "Newer");
                            when(f.messages.findByConversationIdOrderByIdDesc(
                                            10L,
                                            org.springframework.data.domain.PageRequest.of(0, 2)))
                                    .thenReturn(List.of(newer, old));
                            f.send("Hello", "en");
                            verify(f.provider)
                                    .generateReply(
                                            argThat(
                                                    r ->
                                                            r.history()
                                                                    .equals(
                                                                            List.of(
                                                                                    new AiConversationTurn(
                                                                                            MessageSender
                                                                                                    .USER,
                                                                                            "Old"),
                                                                                    new AiConversationTurn(
                                                                                            MessageSender
                                                                                                    .ASSISTANT,
                                                                                            "Newer")))));
                        }),
                integration(
                        "create conversation HTTP stores ownership",
                        () -> {
                            var f = new Fixture();
                            http(f.controller(), "POST", "/api/v1/ai/conversations", "", 200);
                            verify(f.conversations).save(argThat(c -> c.getUser().getId() == 1L));
                        }),
                integration(
                        "rename HTTP persists trimmed title",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller(),
                                    "PATCH",
                                    "/api/v1/ai/conversations/10",
                                    "{\"title\":\" New \"}",
                                    200);
                            assertThat(f.conversation.getTitle()).isEqualTo("New");
                            verify(f.conversations).save(f.conversation);
                        }),
                integration(
                        "delete HTTP removes messages before conversation",
                        () -> {
                            var f = new Fixture();
                            http(f.controller(), "DELETE", "/api/v1/ai/conversations/10", "", 204);
                            var order = inOrder(f.messages, f.conversations);
                            order.verify(f.messages).deleteByConversationId(10L);
                            order.verify(f.messages).flush();
                            order.verify(f.conversations).delete(f.conversation);
                        }),
                integration(
                        "fallback English HTTP never invokes provider",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller(),
                                    "POST",
                                    "/api/v1/ai/conversations/10/messages",
                                    "{\"content\":\"Hello\",\"language\":\"en\"}",
                                    200);
                            verify(f.provider, never()).generateReply(any());
                            verify(f.messages, times(2)).save(any());
                        }),
                integration(
                        "fallback Spanish HTTP bounded output",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller(),
                                    "POST",
                                    "/api/v1/ai/conversations/10/messages",
                                    "{\"content\":\"Hola\",\"language\":\"es\"}",
                                    200);
                            verify(f.messages)
                                    .save(
                                            argThat(
                                                    m ->
                                                            m.getSender() == MessageSender.ASSISTANT
                                                                    && m.getContent().length()
                                                                            <= 8));
                        }),
                integration(
                        "configured limit HTTP rejected before save",
                        () -> {
                            var f = new Fixture();
                            http(
                                    f.controller(),
                                    "POST",
                                    "/api/v1/ai/conversations/10/messages",
                                    "{\"content\":\"123456\",\"language\":\"es\"}",
                                    400);
                            f.noMessage();
                        }),
                integration(
                        "provider error HTTP503 and compensation",
                        () -> {
                            var f = new Fixture();
                            when(f.provider.isAvailable()).thenReturn(true);
                            when(f.provider.generateReply(any()))
                                    .thenThrow(new AiProviderException("synthetic"));
                            http(
                                    f.controller(),
                                    "POST",
                                    "/api/v1/ai/conversations/10/messages",
                                    "{\"content\":\"Hola\",\"language\":\"es\"}",
                                    503);
                            verify(f.messages).delete(any());
                        }),
                integration(
                        "foreign owner history HTTP400",
                        () -> {
                            var f = new Fixture();
                            f.missing();
                            http(
                                    f.controller(),
                                    "GET",
                                    "/api/v1/ai/conversations/10/messages",
                                    "",
                                    400);
                            verifyNoInteractions(f.messages);
                        }));
    }
}
