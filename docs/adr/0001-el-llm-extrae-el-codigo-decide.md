# ADR 0001: El LLM extrae, el código decide

**Estado:** aceptada

## Contexto
Una pre-evaluación crediticia influye en decisiones de negocio y tiene que ser explicable y auditable. Los LLM no son deterministas y pueden equivocarse o inventar datos.

## Decisión
El LLM solo transforma texto libre en datos estructurados (`DatosExtraidos`). Las reglas (relación cuota/ingreso, edad al finalizar) son código Java en `EvaluadorCrediticio`, con tests unitarios.

La salida del modelo se trata como input externo:
- Se fuerza un JSON Schema (tool use en Anthropic, `format` en Ollama).
- Se deserializa con Jackson a un `record` tipado, sin parsear texto libre.
- Se valida con Bean Validation. Si falla, se reintenta una vez y después se rechaza con 502.
- Los campos obligatorios faltantes se calculan en código; no se confía en la lista que devuelve el modelo.

## Consecuencias
- La misma entrada con los mismos datos da siempre la misma decisión.
- Cambiar de modelo no cambia las reglas de negocio.
- Si el modelo extrae mal un dato, la decisión es incorrecta: por eso cada evaluación queda auditada con los datos extraídos.
