package com.renzochirinos.creditoia.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Cuántas llamadas al LLM se permiten en paralelo al procesar un lote.
 * Limita costo y evita superar el rate limit del proveedor.
 */
@Validated
@ConfigurationProperties(prefix = "credito.lote")
public record LoteProperties(@Min(1) @Max(50) int concurrenciaMaxima) {
}
