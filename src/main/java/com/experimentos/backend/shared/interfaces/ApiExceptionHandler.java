package com.experimentos.backend.shared.interfaces;

import com.experimentos.backend.ai.application.AiProviderException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler({
        IllegalArgumentException.class,
        MethodArgumentNotValidException.class,
        MaxUploadSizeExceededException.class,
        MissingServletRequestParameterException.class,
        MissingServletRequestPartException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(Exception exception) {
        return new ErrorResponse("BAD_REQUEST", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    @ResponseStatus(HttpStatus.NOT_IMPLEMENTED)
    public ErrorResponse notImplemented(Exception exception) {
        return new ErrorResponse("NOT_IMPLEMENTED", exception.getMessage(), Instant.now());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse forbidden(AccessDeniedException exception) {
        return new ErrorResponse(
                "FORBIDDEN", "No tienes permisos para realizar esta acción.", Instant.now());
    }

    @ExceptionHandler(AiProviderException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse aiProviderUnavailable() {
        return new ErrorResponse(
                "AI_PROVIDER_UNAVAILABLE",
                "El asistente no está disponible en este momento. Intenta nuevamente.",
                Instant.now());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse serviceUnavailable() {
        return new ErrorResponse(
                "SERVICE_UNAVAILABLE",
                "El servicio no está disponible temporalmente. Intenta nuevamente.",
                Instant.now());
    }

    public record ErrorResponse(String code, String message, Instant timestamp) {}
}
