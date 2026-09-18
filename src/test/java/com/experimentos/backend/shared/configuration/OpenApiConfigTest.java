package com.experimentos.backend.shared.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.junit.jupiter.api.Test;

class OpenApiConfigTest {
    @Test
    void shouldExposeJwtBearerAuthenticationForProtectedOperations() {
        OpenAPI openAPI = new OpenApiConfig().backendOpenAPI();

        SecurityScheme jwtScheme = openAPI.getComponents().getSecuritySchemes().get("bearerAuth");

        assertThat(jwtScheme).isNotNull();
        assertThat(jwtScheme.getType()).isEqualTo(SecurityScheme.Type.HTTP);
        assertThat(jwtScheme.getScheme()).isEqualTo("bearer");
        assertThat(jwtScheme.getBearerFormat()).isEqualTo("JWT");
        assertThat(openAPI.getSecurity()).hasSize(1);
        assertThat(openAPI.getSecurity().getFirst()).containsKey("bearerAuth");
    }
}
