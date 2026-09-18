package com.experimentos.backend.shared.configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    private static final String JWT_SCHEME_NAME = "bearerAuth";

    @Bean
    OpenAPI backendOpenAPI() {
        SecurityScheme jwtScheme =
                new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Enter the JWT returned by the authentication endpoint.");

        return new OpenAPI()
                .info(
                        new Info()
                                .title("SafeSpace Backend API")
                                .version("v1")
                                .description(
                                        "REST API for the SafeSpace employee wellbeing platform."))
                .components(new Components().addSecuritySchemes(JWT_SCHEME_NAME, jwtScheme))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME_NAME));
    }
}
