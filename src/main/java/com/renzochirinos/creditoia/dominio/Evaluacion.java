package com.renzochirinos.creditoia.dominio;

import java.math.BigDecimal;
import java.util.List;

public record Evaluacion(
        ResultadoEvaluacion resultado,
        BigDecimal cuotaEstimada,
        BigDecimal relacionCuotaIngreso,
        List<String> motivos) {

    public Evaluacion {
        motivos = List.copyOf(motivos);
    }

    public static Evaluacion datosInsuficientes(List<String> faltantes) {
        List<String> motivos = faltantes.stream().map(campo -> "Falta el dato: " + campo).toList();
        return new Evaluacion(ResultadoEvaluacion.DATOS_INSUFICIENTES, null, null, motivos);
    }
}
