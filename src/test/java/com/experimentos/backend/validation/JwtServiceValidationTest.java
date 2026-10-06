package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;

import com.experimentos.backend.shared.security.*;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.*;
import java.util.*;
import javax.crypto.SecretKey;

class JwtServiceValidationTest extends ScenarioContract {
    static final String SECRET = "synthetic-test-key-32-bytes-minimum-only";
    static final SecretKey KEY =
            Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8));

    static JwtService service() {
        return new JwtService(SECRET, 60);
    }

    static String signed(String subject, Date expires) {
        return Jwts.builder().subject(subject).expiration(expires).signWith(KEY).compact();
    }

    @Override
    protected List<Scenario> scenarios() {
        return List.of(
                unit(
                        "short secret fails construction",
                        () ->
                                assertThatThrownBy(() -> new JwtService("short", 60))
                                        .isInstanceOf(WeakKeyException.class)),
                unit(
                        "empty secret fails construction",
                        () ->
                                assertThatThrownBy(() -> new JwtService("", 60))
                                        .isInstanceOf(WeakKeyException.class)),
                unit(
                        "absent token rejected",
                        () ->
                                assertThatThrownBy(() -> service().username(null))
                                        .isInstanceOf(IllegalArgumentException.class)),
                unit(
                        "empty token rejected",
                        () ->
                                assertThatThrownBy(() -> service().username(""))
                                        .isInstanceOf(IllegalArgumentException.class)),
                unit(
                        "plain text token rejected",
                        () ->
                                assertThatThrownBy(() -> service().username("not-a-token"))
                                        .isInstanceOf(JwtException.class)),
                unit(
                        "two segments rejected",
                        () ->
                                assertThatThrownBy(() -> service().username("aaa.bbb"))
                                        .isInstanceOf(JwtException.class)),
                unit(
                        "four segments rejected",
                        () ->
                                assertThatThrownBy(() -> service().username("a.b.c.d"))
                                        .isInstanceOf(JwtException.class)),
                unit(
                        "malformed base64 header rejected",
                        () ->
                                assertThatThrownBy(() -> service().username("%%%.e30.c2ln"))
                                        .isInstanceOf(JwtException.class)),
                integration(
                        "employee claims signed",
                        () -> {
                            var u = user(1, Role.EMPLOYEE);
                            var t = service().createToken(u);
                            var p =
                                    Jwts.parser()
                                            .verifyWith(KEY)
                                            .build()
                                            .parseSignedClaims(t)
                                            .getPayload();
                            assertThat(p.getSubject()).isEqualTo("actor");
                            assertThat(p.get("role")).isEqualTo("EMPLOYEE");
                        }),
                integration(
                        "HR claim signed",
                        () -> {
                            var t = service().createToken(user(1, Role.HR_MEMBER));
                            assertThat(
                                            Jwts.parser()
                                                    .verifyWith(KEY)
                                                    .build()
                                                    .parseSignedClaims(t)
                                                    .getPayload()
                                                    .get("role"))
                                    .isEqualTo("HR_MEMBER");
                        }),
                integration(
                        "admin claim signed",
                        () -> {
                            var t = service().createToken(user(1, Role.SYSTEM_ADMIN));
                            assertThat(
                                            Jwts.parser()
                                                    .verifyWith(KEY)
                                                    .build()
                                                    .parseSignedClaims(t)
                                                    .getPayload()
                                                    .get("role"))
                                    .isEqualTo("SYSTEM_ADMIN");
                        }),
                integration(
                        "issued expiry interval exactly configured",
                        () -> {
                            var t = service().createToken(user(1, Role.EMPLOYEE));
                            var p =
                                    Jwts.parser()
                                            .verifyWith(KEY)
                                            .build()
                                            .parseSignedClaims(t)
                                            .getPayload();
                            assertThat(p.getExpiration().getTime() - p.getIssuedAt().getTime())
                                    .isEqualTo(3600000);
                        }),
                integration(
                        "service created token roundtrip",
                        () ->
                                assertThat(
                                                service()
                                                        .username(
                                                                service()
                                                                        .createToken(
                                                                                user(
                                                                                        1,
                                                                                        Role
                                                                                                .EMPLOYEE))))
                                        .isEqualTo("actor")),
                integration(
                        "wrong signing key rejected",
                        () -> {
                            var other =
                                    new JwtService("different-synthetic-key-32-bytes-minimum", 60);
                            assertThatThrownBy(
                                            () ->
                                                    service()
                                                            .username(
                                                                    other.createToken(
                                                                            user(
                                                                                    1,
                                                                                    Role
                                                                                            .EMPLOYEE))))
                                    .isInstanceOf(
                                            io.jsonwebtoken.security.SignatureException.class);
                        }),
                integration(
                        "expired external token rejected",
                        () ->
                                assertThatThrownBy(
                                                () ->
                                                        service()
                                                                .username(
                                                                        signed(
                                                                                "actor",
                                                                                Date.from(
                                                                                        java.time
                                                                                                .Instant
                                                                                                .parse(
                                                                                                        "2000-01-01T00:00:00Z")))))
                                        .isInstanceOf(ExpiredJwtException.class)),
                integration(
                        "future expiration accepted",
                        () ->
                                assertThat(
                                                service()
                                                        .username(
                                                                signed(
                                                                        "actor",
                                                                        Date.from(
                                                                                java.time.Instant
                                                                                        .parse(
                                                                                                "2099-01-01T00:00:00Z")))))
                                        .isEqualTo("actor")),
                integration(
                        "unsigned token rejected",
                        () ->
                                assertThatThrownBy(
                                                () ->
                                                        service()
                                                                .username(
                                                                        Jwts.builder()
                                                                                .subject("actor")
                                                                                .compact()))
                                        .isInstanceOf(JwtException.class)),
                integration(
                        "payload tampering rejected",
                        () -> {
                            var t = service().createToken(user(1, Role.EMPLOYEE));
                            var parts = t.split("\\.");
                            parts[1] =
                                    Base64.getUrlEncoder()
                                            .withoutPadding()
                                            .encodeToString("{\"sub\":\"attacker\"}".getBytes());
                            assertThatThrownBy(() -> service().username(String.join(".", parts)))
                                    .isInstanceOf(JwtException.class);
                        }),
                integration(
                        "signature removed rejected",
                        () -> {
                            var t = service().createToken(user(1, Role.EMPLOYEE));
                            assertThatThrownBy(
                                            () ->
                                                    service()
                                                            .username(
                                                                    t.substring(
                                                                            0,
                                                                            t.lastIndexOf('.')
                                                                                    + 1)))
                                    .isInstanceOf(JwtException.class);
                        }),
                integration(
                        "not before in future rejected",
                        () -> {
                            var t =
                                    Jwts.builder()
                                            .subject("actor")
                                            .notBefore(
                                                    Date.from(
                                                            java.time.Instant.parse(
                                                                    "2099-01-01T00:00:00Z")))
                                            .signWith(KEY)
                                            .compact();
                            assertThatThrownBy(() -> service().username(t))
                                    .isInstanceOf(PrematureJwtException.class);
                        }));
    }
}
