package com.experimentos.backend.authentication.domain;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegistrationPasswordPolicyTest {
    @ParameterizedTest
    @ValueSource(strings = {"Password1!", "Árbol123!", "SafeSpace1🔒"})
    void acceptsPasswordsMeetingTheRequirements(String password) {
        assertThatCode(() -> RegistrationPasswordPolicy.validate(password))
                .doesNotThrowAnyException();
    }
}
