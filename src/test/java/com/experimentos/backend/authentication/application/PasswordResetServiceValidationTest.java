package com.experimentos.backend.authentication.application;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.authentication.domain.*;
import com.experimentos.backend.authentication.infrastructure.*;
import com.experimentos.backend.authentication.interfaces.*;
import com.experimentos.backend.iam.domain.*;
import com.experimentos.backend.iam.infrastructure.*;
import com.experimentos.backend.shared.interfaces.ApiExceptionHandler;
import com.experimentos.backend.shared.security.Role;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PasswordResetServiceValidationTest {
    static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    static final String URL = "https://local.invalid/reset?token=";
    static final String CONFIRM =
            "{\"token\":\"token\",\"new_password\":\"Password1!\",\"confirm_password\":\"Password1!\"}";

    static class Fixture {
        final UserRepository users = mock(UserRepository.class);
        final PasswordResetTokenRepository tokens = mock(PasswordResetTokenRepository.class);
        final PasswordResetNotificationPort notify = mock(PasswordResetNotificationPort.class);
        final PasswordEncoder encoder = mock(PasswordEncoder.class);
        final User actor =
                new User("actor", "actor@example.test", "old-hash", "Actor", Role.EMPLOYEE);
        PasswordResetToken token = new PasswordResetToken(actor, "hash", NOW.plusSeconds(900));
        final PasswordResetProperties properties = new PasswordResetProperties(URL, 15, 3, 15);
        final PasswordResetService service =
                new PasswordResetService(
                        users,
                        tokens,
                        encoder,
                        notify,
                        properties,
                        new PasswordResetRateLimiter(properties, CLOCK),
                        new java.security.SecureRandom(),
                        CLOCK);
        final AuthController controller = new AuthController(mock(AuthService.class), service);

        Fixture() {
            ReflectionTestUtils.setField(actor, "id", 1L);
            when(users.findByUsernameIgnoreCaseOrEmailIgnoreCase("actor", "actor"))
                    .thenReturn(Optional.of(actor));
            when(encoder.encode("Password1!")).thenReturn("encoded-new");
            knownToken();
        }

        void knownToken() {
            when(tokens.findByTokenHash(anyString())).thenReturn(Optional.of(token));
        }

        void noDelivery() {
            verify(tokens, never()).save(any());
            verifyNoInteractions(notify);
        }

        void noChange() {
            verify(users, never()).save(any());
            verify(encoder, never()).encode(anyString());
            assertThat(actor.getPasswordHash()).isEqualTo("old-hash");
        }

        void rejectConfirm(String raw, String password, String confirmation, String message) {
            assertThatThrownBy(
                            () ->
                                    service.confirmRecovery(
                                            new AuthDtos.PasswordResetConfirmRequest(
                                                    raw, password, confirmation)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(message);
        }

        MvcResult http(String suffix, String json, int status) throws Exception {
            var mapper =
                    new ObjectMapper()
                            .findAndRegisterModules()
                            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
            var mvc =
                    MockMvcBuilders.standaloneSetup(controller)
                            .setControllerAdvice(new ApiExceptionHandler())
                            .setMessageConverters(
                                    new org.springframework.http.converter.json
                                            .MappingJackson2HttpMessageConverter(mapper))
                            .build();
            var result =
                    mvc.perform(
                                    MockMvcRequestBuilders.post(
                                                    "/api/v1/auth/password-recovery" + suffix)
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(json))
                            .andReturn();
            assertThat(result.getResponse().getStatus()).isEqualTo(status);
            return result;
        }
    }

    @FunctionalInterface
    interface Checked {
        void run() throws Exception;
    }

    record Case(String name, Checked action) {}

    @TestFactory
    Stream<DynamicTest> unit() {
        return Stream.of(
                        new Case(
                                "null recovery identifier no persistence",
                                () -> {
                                    var f = new Fixture();
                                    assertThat(f.service.requestRecovery(null).message())
                                            .contains("Si la cuenta existe");
                                    verifyNoInteractions(f.users, f.tokens, f.notify);
                                }),
                        new Case(
                                "blank recovery identifier no persistence",
                                () -> {
                                    var f = new Fixture();
                                    f.service.requestRecovery(" \t");
                                    verifyNoInteractions(f.users, f.tokens, f.notify);
                                }),
                        new Case(
                                "HR recovery never sends",
                                () -> {
                                    var f = new Fixture();
                                    f.actor.changeRole(Role.HR_MEMBER);
                                    f.service.requestRecovery("actor");
                                    f.noDelivery();
                                }),
                        new Case(
                                "admin recovery never sends",
                                () -> {
                                    var f = new Fixture();
                                    f.actor.changeRole(Role.SYSTEM_ADMIN);
                                    f.service.requestRecovery("actor");
                                    f.noDelivery();
                                }),
                        new Case(
                                "disabled account recovery never sends",
                                () -> {
                                    var f = new Fixture();
                                    f.actor.disable();
                                    f.service.requestRecovery("actor");
                                    f.noDelivery();
                                }),
                        new Case(
                                "missing email recovery never sends",
                                () -> {
                                    var f = new Fixture();
                                    f.actor.updateProfile("actor", null, "Actor");
                                    f.service.requestRecovery("actor");
                                    f.noDelivery();
                                }),
                        new Case(
                                "blank email recovery never sends",
                                () -> {
                                    var f = new Fixture();
                                    f.actor.updateProfile("actor", " ", "Actor");
                                    f.service.requestRecovery("actor");
                                    f.noDelivery();
                                }),
                        new Case(
                                "token stored hashed not raw",
                                () -> {
                                    var f = new Fixture();
                                    f.service.requestRecovery(" ACTOR ");
                                    var link = org.mockito.ArgumentCaptor.forClass(String.class);
                                    verify(f.notify).send(eq(f.actor), link.capture());
                                    String raw = link.getValue().substring(URL.length());
                                    assertThat(raw).matches("[A-Za-z0-9_-]{43}");
                                    var captor =
                                            org.mockito.ArgumentCaptor.forClass(
                                                    PasswordResetToken.class);
                                    verify(f.tokens).save(captor.capture());
                                    var digest =
                                            java.util.HexFormat.of()
                                                    .formatHex(
                                                            java.security.MessageDigest.getInstance(
                                                                            "SHA-256")
                                                                    .digest(
                                                                            raw.getBytes(
                                                                                    java.nio.charset
                                                                                            .StandardCharsets
                                                                                            .UTF_8)));
                                    assertThat(captor.getValue().matchesHash(digest)).isTrue();
                                    assertThat(captor.getValue().matchesHash(raw)).isFalse();
                                    assertThat(captor.getValue().getExpiresAt())
                                            .isEqualTo(NOW.plusSeconds(900));
                                }),
                        new Case(
                                "missing token rejected before hash change",
                                () -> {
                                    var f = new Fixture();
                                    when(f.tokens.findByTokenHash(anyString()))
                                            .thenReturn(Optional.empty());
                                    f.rejectConfirm(
                                            "token",
                                            "Password1!",
                                            "Password1!",
                                            "Invalid or expired");
                                    f.noChange();
                                }),
                        new Case(
                                "expiration exact boundary unusable",
                                () -> {
                                    var f = new Fixture();
                                    f.token = new PasswordResetToken(f.actor, "hash", NOW);
                                    f.knownToken();
                                    f.rejectConfirm(
                                            "token",
                                            "Password1!",
                                            "Password1!",
                                            "Invalid or expired");
                                    f.noChange();
                                }),
                        new Case(
                                "used token never reusable",
                                () -> {
                                    var f = new Fixture();
                                    f.token.markUsed(NOW.minusSeconds(1));
                                    f.rejectConfirm(
                                            "token",
                                            "Password1!",
                                            "Password1!",
                                            "Invalid or expired");
                                    f.noChange();
                                }),
                        new Case(
                                "confirmation mismatch no change",
                                () -> {
                                    var f = new Fixture();
                                    f.rejectConfirm(
                                            "token",
                                            "Password1!",
                                            "Other1!",
                                            "Passwords do not match");
                                    f.noChange();
                                }))
                .map(c -> DynamicTest.dynamicTest(c.name(), c.action()::run));
    }

    @TestFactory
    Stream<DynamicTest> integration() {
        return Stream.of(
                        new Case(
                                "request recovery controller normalizes identifier",
                                () -> {
                                    var f = new Fixture();
                                    f.http("/request", "{\"identifier\":\" ACTOR \"}", 200);
                                    verify(f.users)
                                            .findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                                    "actor", "actor");
                                    verify(f.notify).send(eq(f.actor), anyString());
                                }),
                        new Case(
                                "unknown account HTTP generic response",
                                () -> {
                                    var f = new Fixture();
                                    when(f.users.findByUsernameIgnoreCaseOrEmailIgnoreCase(
                                                    "actor", "actor"))
                                            .thenReturn(Optional.empty());
                                    var r = f.http("/request", "{\"identifier\":\"actor\"}", 200);
                                    assertThat(r.getResponse().getContentAsString())
                                            .contains("Si la cuenta existe");
                                    f.noDelivery();
                                }),
                        new Case(
                                "confirm HTTP updates password invalidates all tokens",
                                () -> {
                                    var f = new Fixture();
                                    f.http("/confirm", CONFIRM, 200);
                                    assertThat(f.actor.getPasswordHash()).isEqualTo("encoded-new");
                                    verify(f.users).save(f.actor);
                                    verify(f.tokens).deleteByUserId(1L);
                                    assertThat(f.token.getUsedAt()).isEqualTo(NOW);
                                }),
                        new Case(
                                "repeat same token HTTP rejected",
                                () -> {
                                    var f = new Fixture();
                                    f.http("/confirm", CONFIRM, 200);
                                    reset(f.users);
                                    f.http("/confirm", CONFIRM, 400);
                                    verify(f.users, never()).save(any());
                                }),
                        new Case(
                                "expired HTTP token rejected",
                                () -> {
                                    var f = new Fixture();
                                    f.token =
                                            new PasswordResetToken(
                                                    f.actor, "hash", NOW.minusSeconds(1));
                                    f.knownToken();
                                    f.http("/confirm", CONFIRM, 400);
                                    f.noChange();
                                }),
                        new Case(
                                "disabled after issuance HTTP rejected",
                                () -> {
                                    var f = new Fixture();
                                    f.actor.disable();
                                    f.http("/confirm", CONFIRM, 400);
                                    f.noChange();
                                }),
                        new Case(
                                "role changed after issuance HTTP rejected",
                                () -> {
                                    var f = new Fixture();
                                    f.actor.changeRole(Role.HR_MEMBER);
                                    f.http("/confirm", CONFIRM, 400);
                                    f.noChange();
                                }),
                        new Case(
                                "malformed reset HTTP blocked before token lookup",
                                () -> {
                                    var f = new Fixture();
                                    f.http(
                                            "/confirm",
                                            "{\"token\":\"token\",\"new_password\":\"short\",\"confirm_password\":\"short\"}",
                                            400);
                                    verifyNoInteractions(f.tokens);
                                    f.noChange();
                                }))
                .map(c -> DynamicTest.dynamicTest(c.name(), c.action()::run));
    }
}
