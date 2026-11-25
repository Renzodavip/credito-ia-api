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
 * Cliente para Ollama (modelos locales). Usa el parámetro "format" con un JSON Schema
 * para que el modelo devuelva salida estructurada.
 */
@Component
@ConditionalOnProperty(name = "llm.provider", havingValue = "ollama", matchIfMissing = true)
public class OllamaLlmClient implements LlmClient {

    private final RestClient restClient;
    private final LlmProperties props;

    public OllamaLlmClient(RestClient.Builder builder, LlmProperties props) {
        this.props = props;
        this.restClient = builder
                .baseUrl(props.baseUrl())
                .requestFactory(HttpClientes.conTimeout(props.timeout()))
                .build();
    }

    @Override
    public String completarJson(String systemPrompt, String userPrompt, Map<String, Object> esquema) {
        Map<String, Object> cuerpo = Map.of(
                "model", props.model(),
                "stream", false,
                "format", esquema,
                "options", Map.of("temperature", 0),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)));

        JsonNode respuesta;
        try {
            respuesta = restClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(cuerpo)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new LlmNoDisponibleException("Error llamando a Ollama", e);
        }

        String contenido = respuesta == null ? "" : respuesta.path("message").path("content").asText();
        if (contenido.isBlank()) {
            throw new LlmNoDisponibleException("Ollama devolvió una respuesta vacía");
        }
        return contenido;
    }

    @Override
    public String modelo() {
        return "ollama/" + props.model();
    }
}
