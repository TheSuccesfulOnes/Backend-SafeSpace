package com.experimentos.backend.ai.infrastructure;

import com.experimentos.backend.ai.application.AiProperties;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** Creates the bounded HTTP client used by the Gemini adapter. */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class GeminiClientConfiguration {

    @Bean
    @Qualifier("geminiRestClient") RestClient geminiRestClient(RestClient.Builder builder, AiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(30));

        return builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
    }
}
