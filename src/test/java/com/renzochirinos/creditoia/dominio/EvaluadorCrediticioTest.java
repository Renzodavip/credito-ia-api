package com.renzochirinos.creditoia.dominio;

import com.renzochirinos.creditoia.config.ReglasCreditoProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EvaluadorCrediticioTest {

    private final EvaluadorCrediticio evaluador = new EvaluadorCrediticio(
            new ReglasCreditoProperties(new BigDecimal("0.12"), new BigDecimal("0.25"), 75));

    @Test
    void cuotaSistemaFrancesConTasa() {
        // 100.000 al 12% anual en 12 cuotas = 8.884,88 (valor de referencia conocido)
        BigDecimal cuota = EvaluadorCrediticio.cuotaSistemaFrances(
                new BigDecimal("100000"), new BigDecimal("0.12"), 12);
        assertThat(cuota).isEqualByComparingTo("8884.88");
    }

    @Test
    void cuotaSistemaFrancesSinTasa() {
        BigDecimal cuota = EvaluadorCrediticio.cuotaSistemaFrances(
                new BigDecimal("1200000"), BigDecimal.ZERO, 12);
        assertThat(cuota).isEqualByComparingTo("100000");
    }

    @Test
    void precalificaCuandoCumpleTodasLasReglas() {
        Evaluacion resultado = evaluador.evaluar(datos(35, "3000000", "50000000", 240));

        assertThat(resultado.resultado()).isEqualTo(ResultadoEvaluacion.PRECALIFICA);
        assertThat(resultado.motivos()).isEmpty();
        assertThat(resultado.relacionCuotaIngreso()).isLessThanOrEqualTo(new BigDecimal("0.25"));
    }

    @Test
    void noPrecalificaSiLaCuotaSuperaElPorcentajeDelIngreso() {
        Evaluacion resultado = evaluador.evaluar(datos(35, "800000", "50000000", 240));

        assertThat(resultado.resultado()).isEqualTo(ResultadoEvaluacion.NO_PRECALIFICA);
        assertThat(resultado.motivos()).anyMatch(m -> m.contains("25%"));
    }

    @Test
    void noPrecalificaSiLaEdadAlFinalizarSuperaElMaximo() {
        Evaluacion resultado = evaluador.evaluar(datos(60, "5000000", "30000000", 240));

        assertThat(resultado.resultado()).isEqualTo(ResultadoEvaluacion.NO_PRECALIFICA);
        assertThat(resultado.motivos()).anyMatch(m -> m.contains("(80)"));
    }

    @Test
    void datosInsuficientesListaLosCamposFaltantes() {
        DatosExtraidos incompletos = new DatosExtraidos(35, null, new BigDecimal("50000000"), null,
                TipoEmpleo.AUTONOMO, List.of());

        Evaluacion resultado = evaluador.evaluar(incompletos);

        assertThat(resultado.resultado()).isEqualTo(ResultadoEvaluacion.DATOS_INSUFICIENTES);
        assertThat(resultado.motivos())
                .containsExactly("Falta el dato: ingresoMensualNeto", "Falta el dato: plazoMeses");
        assertThat(resultado.cuotaEstimada()).isNull();
    }

    private static DatosExtraidos datos(int edad, String ingreso, String monto, int plazo) {
        return new DatosExtraidos(edad, new BigDecimal(ingreso), new BigDecimal(monto), plazo,
                TipoEmpleo.RELACION_DEPENDENCIA, List.of());
    }
}
