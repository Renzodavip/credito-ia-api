package com.renzochirinos.creditoia.api;

public record ResultadoLote(int indice, EvaluacionResponse evaluacion, String error) {

    public static ResultadoLote ok(int indice, EvaluacionResponse evaluacion) {
        return new ResultadoLote(indice, evaluacion, null);
    }

    public static ResultadoLote fallo(int indice, String error) {
        return new ResultadoLote(indice, null, error);
    }
}
