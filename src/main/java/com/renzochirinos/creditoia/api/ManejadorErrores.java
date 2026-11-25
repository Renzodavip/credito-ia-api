package com.renzochirinos.creditoia.api;

import com.renzochirinos.creditoia.ia.LlmNoDisponibleException;
import com.renzochirinos.creditoia.ia.SalidaLlmInvalidaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Errores con formato RFC 9457 (Problem Details). Los errores de validación del
 * request los resuelve Spring con spring.mvc.problemdetails.enabled=true.
 */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(EvaluacionNoEncontradaException.class)
    ProblemDetail noEncontrada(EvaluacionNoEncontradaException e) {
        return problema(HttpStatus.NOT_FOUND, "Evaluación no encontrada", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail requestInvalido(IllegalArgumentException e) {
        return problema(HttpStatus.BAD_REQUEST, "Request inválido", e.getMessage());
    }

    @ExceptionHandler(LlmNoDisponibleException.class)
    ProblemDetail llmNoDisponible(LlmNoDisponibleException e) {
        log.warn("LLM no disponible", e);
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Servicio de IA no disponible",
                "No se pudo procesar la solicitud en este momento. Reintentá más tarde.");
    }

    @ExceptionHandler(SalidaLlmInvalidaException.class)
    ProblemDetail salidaInvalida(SalidaLlmInvalidaException e) {
        log.warn("Salida inválida del LLM: {}", e.getMessage());
        return problema(HttpStatus.BAD_GATEWAY, "Respuesta inválida del modelo",
                "El modelo no devolvió datos válidos para esta solicitud.");
    }

    private static ProblemDetail problema(HttpStatus estado, String titulo, String detalle) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(estado, detalle);
        problema.setTitle(titulo);
        return problema;
    }
}
