package com.renzochirinos.creditoia.ia;

import com.renzochirinos.creditoia.config.LlmProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClientesLlmTest {

    private static final Map<String, Object> ESQUEMA = Map.of("type", "object");

    @Test
    void anthropicDevuelveElInputDeLaHerramienta() throws Exception {
        String respuesta = """
                {"content":[
                  {"type":"text","text":"Registro los datos."},
                  {"type":"tool_use","name":"registrar_datos","input":{"edad":40,"datosFaltantes":[]}}
                ]}
                """;
        try (ServidorHttpFalso servidor = new ServidorHttpFalso(200, respuesta)) {
            AnthropicLlmClient cliente = new AnthropicLlmClient(RestClient.builder(),
                    new LlmProperties("anthropic", "modelo-test", servidor.url(), "clave-test", Duration.ofSeconds(5)));

            String json = cliente.completarJson("sistema", "usuario", ESQUEMA);

            assertThat(json).contains("\"edad\":40");
            assertThat(servidor.ultimoRequest().ruta()).isEqualTo("/v1/messages");
            assertThat(servidor.ultimoRequest().apiKey()).isEqualTo("clave-test");
            assertThat(servidor.ultimoRequest().cuerpo()).contains("\"tool_choice\"", "\"input_schema\"");
            assertThat(cliente.modelo()).isEqualTo("anthropic/modelo-test");
        }
    }

    @Test
    void anthropicSinApiKeyFallaAlIniciar() {
        LlmProperties sinClave = new LlmProperties("anthropic", "m", "http://localhost", "", Duration.ofSeconds(5));

        assertThatThrownBy(() -> new AnthropicLlmClient(RestClient.builder(), sinClave))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LLM_API_KEY");
    }

    @Test
    void anthropicConvierteErroresHttpEnLlmNoDisponible() throws Exception {
        try (ServidorHttpFalso servidor = new ServidorHttpFalso(529, "{\"error\":\"overloaded\"}")) {
            AnthropicLlmClient cliente = new AnthropicLlmClient(RestClient.builder(),
                    new LlmProperties("anthropic", "m", servidor.url(), "clave", Duration.ofSeconds(5)));

            assertThatThrownBy(() -> cliente.completarJson("s", "u", ESQUEMA))
                    .isInstanceOf(LlmNoDisponibleException.class);
        }
    }

    @Test
    void ollamaDevuelveElContenidoDelMensaje() throws Exception {
        String respuesta = "{\"message\":{\"role\":\"assistant\",\"content\":\"{\\\"edad\\\":30}\"}}";
        try (ServidorHttpFalso servidor = new ServidorHttpFalso(200, respuesta)) {
            OllamaLlmClient cliente = new OllamaLlmClient(RestClient.builder(),
                    new LlmProperties("ollama", "llama3.2", servidor.url(), null, Duration.ofSeconds(5)));

            String json = cliente.completarJson("sistema", "usuario", ESQUEMA);

            assertThat(json).isEqualTo("{\"edad\":30}");
            assertThat(servidor.ultimoRequest().ruta()).isEqualTo("/api/chat");
            assertThat(servidor.ultimoRequest().cuerpo()).contains("\"format\"", "\"stream\":false");
        }
    }

    @Test
    void ollamaConRespuestaVaciaLanzaExcepcion() throws Exception {
        try (ServidorHttpFalso servidor = new ServidorHttpFalso(200, "{\"message\":{\"content\":\"\"}}")) {
            OllamaLlmClient cliente = new OllamaLlmClient(RestClient.builder(),
                    new LlmProperties("ollama", "llama3.2", servidor.url(), null, Duration.ofSeconds(5)));

            assertThatThrownBy(() -> cliente.completarJson("s", "u", ESQUEMA))
                    .isInstanceOf(LlmNoDisponibleException.class);
        }
    }
}
