package com.experimentos.backend.authentication.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

class RegistrationPasswordPolicyValidationTest {
    record Case(String label, String password, boolean valid) {
        @Override
        public String toString() {
            return label;
        }
    }

    static Stream<Case> cases() {
        return Stream.of(
                new Case("null", null, false),
                new Case("empty", "", false),
                new Case("seven codepoints", "Ab1!xyz", false),
                new Case("eight codepoints", "Ab1!xyzz", true),
                new Case("no uppercase", "abcdef1!", false),
                new Case("no lowercase", "ABCDEF1!", false),
                new Case("no digit", "Abcdefg!", false),
                new Case("no special", "Abcdefg1", false),
                new Case("space is not special", "Abcdefg1 ", false),
                new Case("control is not special", "Abcdefg1\n", false),
                new Case("unicode space is not special", "Abcdefg1\u00a0", false),
                new Case("unicode uppercase valid", "Ábcdefg1!", true),
                new Case("unicode decimal digit valid", "Abcdefg١!", true),
                new Case("supplementary symbol valid", "Abcdefg1🔒", true),
                new Case("72 ASCII bytes", "Ab1!" + "x".repeat(68), true),
                new Case("73 ASCII bytes", "Ab1!" + "x".repeat(69), false),
                new Case("72 UTF8 bytes", "Ab1!" + "é".repeat(34), true),
                new Case("74 UTF8 bytes", "Ab1!" + "é".repeat(35), false),
                new Case("surrogate pair is one codepoint", "Ab1!xx🔒", false),
                new Case("zero width control is not special", "Abcdefg1\u200b", false));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    void unit(Case c) {
        if (c.valid())
            assertThatCode(() -> RegistrationPasswordPolicy.validate(c.password()))
                    .doesNotThrowAnyException();
        else
            assertThatThrownBy(() -> RegistrationPasswordPolicy.validate(c.password()))
                    .isInstanceOf(IllegalArgumentException.class);
    }
}
