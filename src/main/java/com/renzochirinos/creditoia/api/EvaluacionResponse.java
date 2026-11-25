package com.renzochirinos.creditoia.api;

import com.renzochirinos.creditoia.dominio.DatosExtraidos;
import com.renzochirinos.creditoia.dominio.ResultadoEvaluacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record EvaluacionResponse(
        UUID id,
        ResultadoEvaluacion resultado,
        BigDecimal cuotaEstimada,
        BigDecimal relacionCuotaIngreso,
        List<String> motivos,
        DatosExtraidos datos,
        String modelo,
        Instant creadoEn) {
}
