# credito-ia-api

[![CI](https://github.com/Renzodavip/credito-ia-api/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Renzodavip/credito-ia-api/actions/workflows/ci.yml)

API de pre-evaluación de solicitudes de crédito hipotecario. Recibe la solicitud del cliente en texto libre, usa un LLM para extraer los datos en un esquema fijo y aplica reglas de negocio en Java para decidir si precalifica.

**Stack:** Java 21 · Spring Boot 3.4 · PostgreSQL · Flyway · JUnit 5 · Mockito · Testcontainers · JaCoCo · Docker · GitHub Actions

## Cómo funciona

```
texto libre ──► enmascarado de PII ──► LLM (salida JSON forzada por esquema)
                                           │
                                           ▼
              Jackson → SalidaLlm → conversión de unidades → Bean Validation
                                           │ (reintento si es inválido)
                                           ▼
                              EvaluadorCrediticio (reglas en Java)
                                           │
                                           ▼
                         auditoría en PostgreSQL ──► respuesta REST
```

Puntos de diseño:

- **El LLM no decide.** Solo extrae datos; la decisión la toman reglas deterministas y testeadas ([ADR 0001](docs/adr/0001-el-llm-extrae-el-codigo-decide.md)).
- **La salida del modelo es input externo.** Esquema forzado, deserialización tipada, validación y reintento.
- **El modelo copia, el código convierte.** El LLM devuelve el número y su unidad (por ejemplo `70` `MILLONES`, `20` `ANIOS`); la conversión a pesos y meses y los topes de validación son código ([ADR 0004](docs/adr/0004-el-modelo-copia-el-codigo-convierte.md)).
- **Los datos personales no salen del perímetro.** DNI, CUIT, CBU, email y teléfono se enmascaran antes de llamar al modelo ([ADR 0002](docs/adr/0002-enmascarado-de-pii.md)).
- **Mitigación de prompt injection.** El texto del cliente va delimitado y no puede cerrar el bloque de datos.
- **Auditoría.** Cada evaluación guarda el texto enmascarado, los datos extraídos, la decisión, el modelo usado, los intentos y la latencia.
- **Idempotencia.** Con el header `Idempotency-Key`, un reintento del cliente devuelve la evaluación existente sin volver a llamar al modelo.
- **Lotes concurrentes.** `/lote` procesa en paralelo con virtual threads y un semáforo que limita las llamadas simultáneas al LLM.
- **Proveedor intercambiable.** Ollama (local) o Anthropic, elegido por configuración ([ADR 0003](docs/adr/0003-restclient-en-lugar-de-framework.md)).
- **Errores RFC 9457** (Problem Details), sin filtrar detalles internos.

## Endpoints

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/v1/evaluaciones` | Evalúa una solicitud. 201 nueva, 200 si la `Idempotency-Key` ya existía |
| GET | `/api/v1/evaluaciones/{id}` | Consulta una evaluación |
| POST | `/api/v1/evaluaciones/lote` | Evalúa hasta 50 solicitudes en paralelo |
| GET | `/actuator/health` | Estado del servicio |

Ejemplos listos para usar en [`requests.http`](requests.http).

## Cómo correrlo

Requisitos: Java 21, Docker y Maven (o IntelliJ, que trae Maven incluido).

```bash
# 1. Base de datos
docker compose up -d db

# 2. LLM local (opción gratis)
ollama pull llama3.2

# 3. Aplicación
mvn spring-boot:run
```

Para usar Anthropic en lugar de Ollama, definí `LLM_PROVIDER=anthropic`, `LLM_MODEL`, `LLM_BASE_URL=https://api.anthropic.com` y `LLM_API_KEY` (ver `.env.example`).

Todo en contenedores: `cp .env.example .env` y `docker compose up --build`.

### Rendimiento y hardware

Con Ollama la velocidad depende del equipo. En una máquina sin GPU (solo CPU) medí alrededor de 1,5 a 2 tokens por segundo, lo que se traduce en cerca de 1 minuto por solicitud con `llama3.2` (3B) y más de 2 minutos con `llama3.1:8b`. Con una GPU o con un proveedor en la nube las respuestas suelen tardar segundos.

Si el modelo tarda más que el timeout (60 s por defecto), la API responde 503 con un Problem Details. Para equipos lentos:

```bash
LLM_TIMEOUT=240s    # tiempo máximo de espera por llamada al modelo
```

En `/lote`, las llamadas simultáneas al modelo se limitan con `credito.lote.concurrencia-maxima` (4 por defecto). En CPU conviene bajarlo, porque las llamadas en paralelo compiten por el mismo procesador y se enlentecen entre sí.

El modelo chico también comete errores de extracción (por ejemplo, elegir mal la unidad del plazo); ver el límite conocido en el [ADR 0004](docs/adr/0004-el-modelo-copia-el-codigo-convierte.md).

## Tests

```bash
mvn verify
```

Corre tests unitarios, de controller (`@WebMvcTest`) y de integración con PostgreSQL real (Testcontainers, necesita Docker). El build falla si la cobertura de líneas baja del 70% (JaCoCo). El reporte queda en `target/site/jacoco/index.html`.

## Próximos pasos

- Métricas de tokens, latencia y costo por endpoint con Micrometer.
- Circuit breaker y retry con backoff (Resilience4j) para el proveedor de LLM.
- Set de evaluación ("golden set") para medir la calidad de la extracción al cambiar de modelo o de prompt.
