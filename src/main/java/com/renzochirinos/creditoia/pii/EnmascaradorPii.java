package com.renzochirinos.creditoia.pii;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Enmascara datos personales antes de que el texto salga del perímetro hacia el LLM.
 * DNI y teléfono solo se enmascaran cuando vienen después de una palabra clave,
 * para no confundirlos con montos como "20.000.000".
 */
@Component
public class EnmascaradorPii {

    private record Regla(Pattern patron, String reemplazo) {
    }

    private static final List<Regla> REGLAS = List.of(
            new Regla(Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+"), "[EMAIL]"),
            new Regla(Pattern.compile("\\b\\d{22}\\b"), "[CBU]"),
            new Regla(Pattern.compile("\\b(20|23|24|27|30|33|34)-?\\d{8}-?\\d\\b"), "[CUIT]"),
            new Regla(Pattern.compile("(?i)\\b(dni|documento)([^\\d\\n]{0,15}?)\\d{1,2}\\.?\\d{3}\\.?\\d{3}\\b"),
                    "$1$2[DNI]"),
            new Regla(Pattern.compile("(?i)\\b(tel[eé]fono|tel|celular|cel|whatsapp)([^\\d\\n+]{0,15}?)\\+?\\d[\\d\\s()-]{6,}\\d"),
                    "$1$2[TELEFONO]"),
            new Regla(Pattern.compile("\\+54[\\d\\s()-]{8,}\\d"), "[TELEFONO]"));

    public String enmascarar(String texto) {
        String resultado = texto;
        for (Regla regla : REGLAS) {
            resultado = regla.patron().matcher(resultado).replaceAll(regla.reemplazo());
        }
        return resultado;
    }
}
