package com.renzochirinos.creditoia.dominio;

import com.renzochirinos.creditoia.config.ReglasCreditoProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas deterministas de pre-evaluación. El LLM nunca decide: solo extrae datos.
 * La decisión se toma acá, con código testeable y auditable.
 */
@Component
public class EvaluadorCrediticio {

    private static final BigDecimal DOCE = BigDecimal.valueOf(12);

    private final ReglasCreditoProperties reglas;

    public EvaluadorCrediticio(ReglasCreditoProperties reglas) {
        this.reglas = reglas;
    }

    public Evaluacion evaluar(DatosExtraidos datos) {
        List<String> faltantes = datos.camposObligatoriosFaltantes();
        if (!faltantes.isEmpty()) {
            return Evaluacion.datosInsuficientes(faltantes);
        }

        BigDecimal cuota = cuotaSistemaFrances(datos.montoSolicitado(), reglas.tasaNominalAnual(), datos.plazoMeses());
        BigDecimal relacion = cuota.divide(datos.ingresoMensualNeto(), 4, RoundingMode.HALF_UP);

        List<String> motivos = new ArrayList<>();
        if (relacion.compareTo(reglas.relacionCuotaIngresoMaxima()) > 0) {
            motivos.add("La cuota estimada supera el %s%% del ingreso neto"
                    .formatted(reglas.relacionCuotaIngresoMaxima().movePointRight(2).stripTrailingZeros().toPlainString()));
        }
        int edadAlFinalizar = datos.edad() + (int) Math.ceil(datos.plazoMeses() / 12.0);
        if (edadAlFinalizar > reglas.edadMaximaAlFinalizar()) {
            motivos.add("La edad al finalizar el crédito (%d) supera el máximo de %d"
                    .formatted(edadAlFinalizar, reglas.edadMaximaAlFinalizar()));
        }

        ResultadoEvaluacion resultado = motivos.isEmpty()
                ? ResultadoEvaluacion.PRECALIFICA
                : ResultadoEvaluacion.NO_PRECALIFICA;
        return new Evaluacion(resultado, cuota, relacion, motivos);
    }

    /** Cuota fija mensual (sistema francés). */
    static BigDecimal cuotaSistemaFrances(BigDecimal monto, BigDecimal tasaNominalAnual, int meses) {
        if (tasaNominalAnual.signum() == 0) {
            return monto.divide(BigDecimal.valueOf(meses), 2, RoundingMode.HALF_UP);
        }
        MathContext mc = MathContext.DECIMAL64;
        BigDecimal tasaMensual = tasaNominalAnual.divide(DOCE, mc);
        BigDecimal factor = BigDecimal.ONE.add(tasaMensual).pow(meses, mc);
        return monto.multiply(tasaMensual, mc)
                .multiply(factor, mc)
                .divide(factor.subtract(BigDecimal.ONE), mc)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
