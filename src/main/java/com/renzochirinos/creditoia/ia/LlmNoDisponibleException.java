package com.renzochirinos.creditoia.ia;

public class LlmNoDisponibleException extends RuntimeException {

    public LlmNoDisponibleException(String mensaje) {
        super(mensaje);
    }

    public LlmNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
