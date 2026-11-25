# ADR 0003: RestClient directo en lugar de Spring AI o LangChain4j

**Estado:** aceptada

## Contexto
El servicio hace una sola cosa con el LLM: una llamada con salida estructurada. No hay conversación, memoria, RAG ni varias herramientas.

## Decisión
Usar `RestClient` (Spring 6.1) sobre `java.net.http.HttpClient`, con timeouts de conexión y lectura explícitos, detrás de una interfaz propia `LlmClient` con dos implementaciones (Anthropic y Ollama) elegidas por configuración.

## Consecuencias
- Control total sobre el request, los timeouts y el manejo de errores, sin dependencias extra.
- Cambiar de proveedor es agregar una implementación de `LlmClient`.
- Para el proyecto del agente (tool calling de varios pasos, MCP) sí conviene un framework: ahí se usa Spring AI.
