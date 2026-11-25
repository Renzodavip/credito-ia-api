package com.renzochirinos.creditoia.dominio;

import java.math.BigDecimal;

/**
 * Unidad en la que el cliente expresó un monto ("2.800.000" son PESOS, "70 millones" son MILLONES).
 * El modelo solo copia el número y la unidad; la conversión a pesos la hace este enum.
 */
public enum UnidadMonto {
    PESOS(1),
    MILES(1_000),
    MILLONES(1_000_000);

    private final BigDecimal factor;

    UnidadMonto(long factor) {
        this.factor = BigDecimal.valueOf(factor);
    }

    public BigDecimal aPesos(BigDecimal valor) {
        return valor.multiply(factor);
    }
}
