package com.renzochirinos.creditoia.ia;

import java.util.Map;

/**
 * Puerto hacia el proveedor de LLM. El resto de la aplicación no sabe si atrás
 * hay Ollama, Anthropic u otro: solo pide JSON que cumpla un esquema.
 */
public interface LlmClient {

    /**
     * @param systemPrompt instrucciones fijas del sistema
     * @param userPrompt   contenido del usuario (ya enmascarado)
     * @param esquema      JSON Schema que la respuesta debe cumplir
     * @return el JSON devuelto por el modelo, sin parsear
     */
    String completarJson(String systemPrompt, String userPrompt, Map<String, Object> esquema);

    String modelo();
}
