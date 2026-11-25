package com.renzochirinos.creditoia.ia;

import com.renzochirinos.creditoia.dominio.DatosExtraidos;
import com.renzochirinos.creditoia.dominio.TipoEmpleo;
import com.renzochirinos.creditoia.dominio.UnidadMonto;
import com.renzochirinos.creditoia.dominio.UnidadPlazo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que devuelve el modelo, sin convertir: el número tal como aparece en el texto y su unidad.
 * Las conversiones a pesos y a meses las hace {@link #aDatos()}, en código y no en el modelo.
 */
record SalidaLlm(
        Integer edad,
        BigDecimal ingresoValor,
        UnidadMonto ingresoUnidad,
        BigDecimal montoValor,
        UnidadMonto montoUnidad,
        BigDecimal plazoValor,
        UnidadPlazo plazoUnidad,
        TipoEmpleo tipoEmpleo,
        List<String> datosFaltantes) {

    /** @throws IllegalArgumentException si hay un valor sin su unidad (no se adivina la unidad). */
    DatosExtraidos aDatos() {
        BigDecimal ingreso = ingresoValor == null ? null
                : exigirUnidad(ingresoUnidad, "ingresoUnidad").aPesos(ingresoValor);
        BigDecimal monto = montoValor == null ? null
                : exigirUnidad(montoUnidad, "montoUnidad").aPesos(montoValor);
        Integer plazoMeses = plazoValor == null ? null
                : exigirUnidad(plazoUnidad, "plazoUnidad").aMeses(plazoValor);
        return new DatosExtraidos(edad, ingreso, monto, plazoMeses, tipoEmpleo, datosFaltantes);
    }

    private static <T> T exigirUnidad(T unidad, String campo) {
        if (unidad == null) {
            throw new IllegalArgumentException(campo + " es obligatoria cuando se informa el valor");
        }
        return unidad;
    }
}
