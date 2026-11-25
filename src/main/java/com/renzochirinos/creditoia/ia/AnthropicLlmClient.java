package com.renzochirinos.creditoia.ia;

import com.fasterxml.jackson.databind.JsonNode;
import com.renzochirinos.creditoia.config.LlmProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

/**
 * Cliente para la Messages API de Anthropic. Fuerza la salida estructurada
 * obligando al modelo a usar una herramienta cuyo input_schema es el esquema pedido.
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "anthropic")
public class AnthropicLlmClient implements LlmClient {

    private static final String HERRAMIENTA = "registrar_datos";
    private static final String VERSION_API = "2023-06-01";

    private final RestClient restClient;
    private final LlmProperties props;

    public AnthropicLlmClient(RestClient.Builder builder, LlmProperties props) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new IllegalStateException("Con llm.provider=anthropic hay que definir la variable LLM_API_KEY");
        }
        this.props = props;
        this.restClient = builder
                .baseUrl(props.baseUrl())
                .requestFactory(HttpClientes.conTimeout(props.timeout()))
                .defaultHeader("x-api-key", props.apiKey())
                .defaultHeader("anthropic-version", VERSION_API)
                .build();
    }

    @Override
    public String completarJson(String systemPrompt, String userPrompt, Map<String, Object> esquema) {
        Map<String, Object> cuerpo = Map.of(
                "model", props.model(),
                "max_tokens", 1024,
                "temperature", 0,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userPrompt)),
                "tools", List.of(Map.of(
                        "name", HERRAMIENTA,
                        "description", "Registra los datos extraídos de la solicitud de crédito",
                        "input_schema", esquema)),
                "tool_choice", Map.of("type", "tool", "name", HERRAMIENTA));

        JsonNode respuesta;
        try {
            respuesta = restClient.post()
                    .uri("/v1/messages")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new LlmNoDisponibleException("Error llamando a la API de Anthropic", e);
        }

        if (respuesta != null) {
            for (JsonNode bloque : respuesta.path("content")) {
                if ("tool_use".equals(bloque.path("type").asText())) {
                    return bloque.path("input").toString();
                }
            }
        }
        throw new LlmNoDisponibleException("La respuesta de Anthropic no incluyó la herramienta esperada");
    }

    @Override
    public String modelo() {
        return "anthropic/" + props.model();
    }
}
