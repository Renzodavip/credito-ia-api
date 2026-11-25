package com.renzochirinos.creditoia.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Unidad en la que el cliente expresó el plazo ("20 años" es ANIOS). El modelo solo copia el número
 * y la unidad; la conversión a meses la hace este enum.
 */
public enum UnidadPlazo {
    MESES(1),
    ANIOS(12);

    private static final BigDecimal TOPE = BigDecimal.valueOf(Integer.MAX_VALUE);

    private final BigDecimal mesesPorUnidad;

    UnidadPlazo(long mesesPorUnidad) {
        this.mesesPorUnidad = BigDecimal.valueOf(mesesPorUnidad);
    }

    /** Redondea al mes más cercano. Los valores absurdos se acotan; el rango lo valida Bean Validation. */
    public int aMeses(BigDecimal valor) {
        BigDecimal meses = valor.multiply(mesesPorUnidad).setScale(0, RoundingMode.HALF_UP);
        return meses.max(TOPE.negate()).min(TOPE).intValue();
    }
}
