package com.renzochirinos.creditoia.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Reglas de negocio de la pre-evaluación. Viven en configuración para poder
 * ajustarlas por ambiente sin tocar código.
 */
@Validated
@ConfigurationProperties(prefix = "credito.reglas")
public record ReglasCreditoProperties(
        @NotNull @DecimalMin("0.0") @DecimalMax("2.0") BigDecimal tasaNominalAnual,
        @NotNull @DecimalMin("0.01") @DecimalMax("1.0") BigDecimal relacionCuotaIngresoMaxima,
        @Min(18) @Max(100) int edadMaximaAlFinalizar) {
}
