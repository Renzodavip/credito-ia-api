package com.renzochirinos.creditoia.dominio;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class UnidadesTest {

    @Test
    void montosSeConviertenAPesos() {
        assertThat(UnidadMonto.PESOS.aPesos(new BigDecimal("2800000"))).isEqualByComparingTo("2800000");
        assertThat(UnidadMonto.MILES.aPesos(new BigDecimal("2800"))).isEqualByComparingTo("2800000");
        assertThat(UnidadMonto.MILLONES.aPesos(new BigDecimal("70"))).isEqualByComparingTo("70000000");
    }

    @Test
    void montosConDecimalesNoPierdenPrecision() {
        assertThat(UnidadMonto.MILLONES.aPesos(new BigDecimal("2.5"))).isEqualByComparingTo("2500000");
        assertThat(UnidadMonto.MILES.aPesos(new BigDecimal("1.5"))).isEqualByComparingTo("1500");
    }

    @Test
    void plazosSeConviertenAMeses() {
        assertThat(UnidadPlazo.ANIOS.aMeses(new BigDecimal("20"))).isEqualTo(240);
        assertThat(UnidadPlazo.MESES.aMeses(new BigDecimal("180"))).isEqualTo(180);
    }

    @Test
    void plazosEnAniosConDecimalSeRedondeanAlMes() {
        assertThat(UnidadPlazo.ANIOS.aMeses(new BigDecimal("1.5"))).isEqualTo(18);
        assertThat(UnidadPlazo.MESES.aMeses(new BigDecimal("12.4"))).isEqualTo(12);
        assertThat(UnidadPlazo.MESES.aMeses(new BigDecimal("12.5"))).isEqualTo(13);
    }

    @Test
    void plazosAbsurdosSeAcotanSinDesbordar() {
        assertThat(UnidadPlazo.ANIOS.aMeses(new BigDecimal("99999999999999"))).isEqualTo(Integer.MAX_VALUE);
        assertThat(UnidadPlazo.ANIOS.aMeses(new BigDecimal("-99999999999999"))).isEqualTo(-Integer.MAX_VALUE);
    }
}
