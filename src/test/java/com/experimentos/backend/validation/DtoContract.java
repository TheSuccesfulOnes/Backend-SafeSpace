package com.experimentos.backend.validation;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.experimentos.backend.shared.interfaces.ApiExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/** Contract tests execute the real DTO constraints and the actual controller binding pipeline. */
abstract class DtoContract {
    protected record Case(
            String name,
            Class<?> dto,
            String method,
            String path,
            String json,
            String violation,
            boolean integration) {}

    protected abstract List<Case> cases();

    protected abstract Object controller();

    protected abstract List<Object> services();

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final ObjectMapper httpMapper =
            new ObjectMapper()
                    .findAndRegisterModules()
                    .setPropertyNamingStrategy(
                            com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);

    private com.fasterxml.jackson.databind.JsonNode snakeKeys(
            com.fasterxml.jackson.databind.JsonNode node) {
        if (node.isObject()) {
            var renamed = httpMapper.createObjectNode();
            node.properties()
                    .forEach(
                            e ->
                                    renamed.set(
                                            new com.fasterxml.jackson.databind
                                                            .PropertyNamingStrategies
                                                            .SnakeCaseStrategy()
                                                    .translate(e.getKey()),
                                            snakeKeys(e.getValue())));
            return renamed;
        }
        if (node.isArray()) {
            var array = httpMapper.createArrayNode();
            node.forEach(n -> array.add(snakeKeys(n)));
            return array;
        }
        return node;
    }

    private String httpJson(String json) {
        try {
            return httpMapper.writeValueAsString(snakeKeys(mapper.readTree(json)));
        } catch (Exception malformedJson) {
            return json;
        }
    }

    @TestFactory
    Stream<DynamicTest> unit() {
        return cases().stream()
                .filter(c -> !c.integration())
                .map(
                        c ->
                                DynamicTest.dynamicTest(
                                        c.name(),
                                        () -> {
                                            try (ValidatorFactory factory =
                                                    Validation.buildDefaultValidatorFactory()) {
                                                Object dto = mapper.readValue(c.json(), c.dto());
                                                var violations =
                                                        factory.getValidator().validate(dto);
                                                if (c.violation() == null)
                                                    assertThat(violations).isEmpty();
                                                else
                                                    assertThat(violations)
                                                            .anySatisfy(
                                                                    v ->
                                                                            assertThat(
                                                                                            v.getPropertyPath()
                                                                                                    .toString())
                                                                                    .startsWith(
                                                                                            c
                                                                                                    .violation()));
                                            }
                                        }));
    }

    @TestFactory
    Stream<DynamicTest> integration() {
        return cases().stream()
                .filter(Case::integration)
                .map(
                        c ->
                                DynamicTest.dynamicTest(
                                        c.name(),
                                        () -> {
                                            List<Object> mocks = services();
                                            mocks.forEach(org.mockito.Mockito::reset);
                                            org.springframework.security.core.context
                                                    .SecurityContextHolder.clearContext();
                                            try {
                                                MockMvc mvc =
                                                        MockMvcBuilders.standaloneSetup(
                                                                        controller())
                                                                .setMessageConverters(
                                                                        new org.springframework.http
                                                                                .converter.json
                                                                                .MappingJackson2HttpMessageConverter(
                                                                                httpMapper))
                                                                .setControllerAdvice(
                                                                        new ApiExceptionHandler())
                                                                .build();
                                                var result =
                                                        mvc.perform(
                                                                        MockMvcRequestBuilders
                                                                                .request(
                                                                                        org
                                                                                                .springframework
                                                                                                .http
                                                                                                .HttpMethod
                                                                                                .valueOf(
                                                                                                        c
                                                                                                                .method()),
                                                                                        c.path())
                                                                                .contentType(
                                                                                        MediaType
                                                                                                .APPLICATION_JSON)
                                                                                .content(
                                                                                        httpJson(
                                                                                                c
                                                                                                        .json())))
                                                                .andReturn();
                                                if (c.violation() != null) {
                                                    assertThat(result.getResponse().getStatus())
                                                            .isEqualTo(400);
                                                    mocks.forEach(
                                                            org.mockito.Mockito
                                                                    ::verifyNoInteractions);
                                                } else {
                                                    assertThat(result.getResponse().getStatus())
                                                            .isBetween(200, 299);
                                                    assertThat(
                                                                    mocks.stream()
                                                                            .flatMap(
                                                                                    m ->
                                                                                            mockingDetails(
                                                                                                    m)
                                                                                                    .getInvocations()
                                                                                                    .stream())
                                                                            .count())
                                                            .isEqualTo(1);
                                                    Object decoded =
                                                            mapper.readValue(c.json(), c.dto());
                                                    assertThat(
                                                                    mocks.stream()
                                                                            .flatMap(
                                                                                    m ->
                                                                                            mockingDetails(
                                                                                                    m)
                                                                                                    .getInvocations()
                                                                                                    .stream())
                                                                            .flatMap(
                                                                                    i ->
                                                                                            java
                                                                                                    .util
                                                                                                    .Arrays
                                                                                                    .stream(
                                                                                                            i
                                                                                                                    .getArguments()))
                                                                            .toList())
                                                            .contains(
                                                                    decoded
                                                                                    instanceof
                                                                                    com.experimentos
                                                                                                            .backend
                                                                                                            .authentication
                                                                                                            .interfaces
                                                                                                            .AuthDtos
                                                                                                            .PasswordRecoveryRequest
                                                                                                    r
                                                                            ? r.identifier()
                                                                            : decoded);
                                                }
                                            } finally {
                                                org.springframework.security.core.context
                                                        .SecurityContextHolder.clearContext();
                                                mocks.forEach(org.mockito.Mockito::reset);
                                            }
                                        }));
    }
}
