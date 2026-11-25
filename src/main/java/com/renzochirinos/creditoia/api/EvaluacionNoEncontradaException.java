package com.renzochirinos.creditoia.api;

import java.util.UUID;

public class EvaluacionNoEncontradaException extends RuntimeException {

    public EvaluacionNoEncontradaException(UUID id) {
        super("No existe la evaluación " + id);
    }
}
