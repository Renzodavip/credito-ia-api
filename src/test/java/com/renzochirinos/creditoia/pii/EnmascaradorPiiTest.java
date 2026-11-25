package com.renzochirinos.creditoia.pii;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class EnmascaradorPiiTest {

    private final EnmascaradorPii enmascarador = new EnmascaradorPii();

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', textBlock = """
            Mi DNI es 30.123.456                   | Mi DNI es [DNI]
            documento: 30123456                    | documento: [DNI]
            escribime a renzo.test@mail.com        | escribime a [EMAIL]
            mi CUIT es 20-30123456-7               | mi CUIT es [CUIT]
            CUIL 27301234567                       | CUIL [CUIT]
            mi cel es 11 3093-8276                 | mi cel es [TELEFONO]
            llamame al +54 9 11 3093-8276          | llamame al [TELEFONO]
            CBU 0110599520000001234567             | CBU [CBU]
            """)
    void enmascaraDatosPersonales(String entrada, String esperado) {
        assertThat(enmascarador.enmascarar(entrada)).isEqualTo(esperado);
    }

    @Test
    void noEnmascaraMontosNiPlazos() {
        String texto = "Quiero 20.000.000 de pesos a 240 meses y cobro 1500000 por mes";
        assertThat(enmascarador.enmascarar(texto)).isEqualTo(texto);
    }

    @Test
    void enmascaraVariosDatosEnElMismoTexto() {
        String resultado = enmascarador.enmascarar(
                "Soy Ana, DNI 28.555.111, mail ana@banco.com, pido 50.000.000");

        assertThat(resultado)
                .contains("[DNI]", "[EMAIL]", "50.000.000")
                .doesNotContain("28.555.111", "ana@banco.com");
    }
}
