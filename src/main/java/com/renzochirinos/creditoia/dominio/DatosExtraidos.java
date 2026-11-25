package com.renzochirinos.creditoia.dominio;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Datos que el LLM extrae del texto libre. Todos los campos pueden venir en null
 * si el cliente no los mencionó: el modelo no debe inventarlos.
 * Las anotaciones validan la salida del modelo como cualquier input externo.
 */
public record DatosExtraidos(
        @Min(18) @Max(100) Integer edad,
        @DecimalMin("50000") @DecimalMax("100000000") BigDecimal ingresoMensualNeto,
        @DecimalMin("1000000") @DecimalMax("5000000000") BigDecimal montoSolicitado,
        @Min(12) @Max(480) Integer plazoMeses,
        TipoEmpleo tipoEmpleo,
        List<String> datosFaltantes) {

    /** Únicos nombres válidos en datosFaltantes; cualquier otro que invente el modelo se descarta. */
    public static final Set<String> CAMPOS_EXTRAIBLES =
            Set.of("edad", "ingresoMensualNeto", "montoSolicitado", "plazoMeses", "tipoEmpleo");

    public DatosExtraidos {
        datosFaltantes = datosFaltantes == null ? List.of()
                : datosFaltantes.stream().filter(CAMPOS_EXTRAIBLES::contains).distinct().toList();
        tipoEmpleo = tipoEmpleo == null ? TipoEmpleo.DESCONOCIDO : tipoEmpleo;
    }

    /** Campos obligatorios ausentes, calculados por código (no confiamos en la lista del modelo). */
    public List<String> camposObligatoriosFaltantes() {
        List<String> faltantes = new ArrayList<>();
        if (edad == null) faltantes.add("edad");
        if (ingresoMensualNeto == null) faltantes.add("ingresoMensualNeto");
        if (montoSolicitado == null) faltantes.add("montoSolicitado");
        if (plazoMeses == null) faltantes.add("plazoMeses");
        return faltantes;
    }
}
