package com.experimentos.backend.ai.application;

/** Internal error raised when the configured AI provider cannot answer safely. */
public class AiProviderException extends RuntimeException {

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiProviderException(String message) {
        super(message);
    }
}
