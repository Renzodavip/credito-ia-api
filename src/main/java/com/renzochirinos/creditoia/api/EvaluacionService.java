package com.renzochirinos.creditoia.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.renzochirinos.creditoia.config.LoteProperties;
import com.renzochirinos.creditoia.dominio.DatosExtraidos;
import com.renzochirinos.creditoia.dominio.Evaluacion;
import com.renzochirinos.creditoia.dominio.EvaluadorCrediticio;
import com.renzochirinos.creditoia.ia.Extraccion;
import com.renzochirinos.creditoia.ia.ExtractorDatos;
import com.renzochirinos.creditoia.ia.LlmClient;
import com.renzochirinos.creditoia.persistencia.EvaluacionEntity;
import com.renzochirinos.creditoia.persistencia.EvaluacionRepository;
import com.renzochirinos.creditoia.pii.EnmascaradorPii;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

@Service
public class EvaluacionService {

    /** Resultado de una evaluación y si se reutilizó una anterior por Idempotency-Key. */
    public record EvaluacionGuardada(EvaluacionResponse respuesta, boolean reutilizada) {
    }

    private final EnmascaradorPii enmascarador;
    private final ExtractorDatos extractor;
    private final EvaluadorCrediticio evaluador;
    private final EvaluacionRepository repositorio;
    private final LlmClient llm;
    private final ObjectMapper objectMapper;
    private final LoteProperties lote;

    public EvaluacionService(EnmascaradorPii enmascarador, ExtractorDatos extractor,
                             EvaluadorCrediticio evaluador, EvaluacionRepository repositorio,
                             LlmClient llm, ObjectMapper objectMapper, LoteProperties lote) {
        this.enmascarador = enmascarador;
        this.extractor = extractor;
        this.evaluador = evaluador;
        this.repositorio = repositorio;
        this.llm = llm;
        this.objectMapper = objectMapper;
        this.lote = lote;
    }

    /**
     * La llamada al LLM se hace fuera de cualquier transacción: no tiene sentido
     * tener una conexión de base tomada mientras esperamos varios segundos al modelo.
     */
    public EvaluacionGuardada evaluar(String texto, String idempotencyKey) {
        if (idempotencyKey != null && idempotencyKey.length() > 100) {
            throw new IllegalArgumentException("Idempotency-Key no puede superar 100 caracteres");
        }
        if (idempotencyKey != null) {
            Optional<EvaluacionEntity> existente = repositorio.findByIdempotencyKey(idempotencyKey);
            if (existente.isPresent()) {
                return new EvaluacionGuardada(aRespuesta(existente.get()), true);
            }
        }

        long inicio = System.nanoTime();
        String enmascarado = enmascarador.enmascarar(texto);
        Extraccion extraccion = extractor.extraer(enmascarado);
        Evaluacion evaluacion = evaluador.evaluar(extraccion.datos());
        long latenciaMs = Duration.ofNanos(System.nanoTime() - inicio).toMillis();

        EvaluacionEntity entidad = new EvaluacionEntity(
                idempotencyKey,
                Instant.now(),
                enmascarado,
                aJson(extraccion.datos()),
                evaluacion.resultado(),
                evaluacion.cuotaEstimada(),
                evaluacion.relacionCuotaIngreso(),
                String.join("\n", evaluacion.motivos()),
                llm.modelo(),
                extraccion.intentos(),
                latenciaMs);

        try {
            return new EvaluacionGuardada(aRespuesta(repositorio.save(entidad)), false);
        } catch (DataIntegrityViolationException e) {
            // Dos requests con la misma Idempotency-Key en paralelo: gana la primera.
            if (idempotencyKey == null) {
                throw e;
            }
            return repositorio.findByIdempotencyKey(idempotencyKey)
                    .map(guardada -> new EvaluacionGuardada(aRespuesta(guardada), true))
                    .orElseThrow(() -> e);
        }
    }

    public EvaluacionResponse obtener(UUID id) {
        return repositorio.findById(id)
                .map(this::aRespuesta)
                .orElseThrow(() -> new EvaluacionNoEncontradaException(id));
    }

    /**
     * Procesa un lote en paralelo con virtual threads. El semáforo limita cuántas
     * llamadas al LLM hay en vuelo a la vez (costo y rate limit del proveedor).
     * Un error en una solicitud no corta el lote: se informa en su posición.
     */
    public List<ResultadoLote> evaluarLote(List<String> textos) {
        Semaphore permisos = new Semaphore(lote.concurrenciaMaxima());
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<ResultadoLote>> futuros = new ArrayList<>();
            for (int i = 0; i < textos.size(); i++) {
                int indice = i;
                String texto = textos.get(i);
                futuros.add(CompletableFuture.supplyAsync(() -> procesarConPermiso(permisos, indice, texto), executor));
            }
            CompletableFuture.allOf(futuros.toArray(new CompletableFuture[0])).join();
            return futuros.stream().map(CompletableFuture::join).toList();
        }
    }

    private ResultadoLote procesarConPermiso(Semaphore permisos, int indice, String texto) {
        try {
            permisos.acquire();
            try {
                return ResultadoLote.ok(indice, evaluar(texto, null).respuesta());
            } finally {
                permisos.release();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResultadoLote.fallo(indice, "Procesamiento interrumpido");
        } catch (RuntimeException e) {
            return ResultadoLote.fallo(indice, e.getMessage());
        }
    }

    private EvaluacionResponse aRespuesta(EvaluacionEntity e) {
        List<String> motivos = e.getMotivos() == null || e.getMotivos().isBlank()
                ? List.of()
                : Arrays.asList(e.getMotivos().split("\n"));
        return new EvaluacionResponse(
                e.getId(),
                e.getResultado(),
                e.getCuotaEstimada(),
                e.getRelacionCuotaIngreso(),
                motivos,
                deJson(e.getDatosExtraidosJson()),
                e.getModelo(),
                e.getCreadoEn());
    }

    private String aJson(DatosExtraidos datos) {
        try {
            return objectMapper.writeValueAsString(datos);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudieron serializar los datos extraídos", e);
        }
    }

    private DatosExtraidos deJson(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, DatosExtraidos.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Datos extraídos corruptos en base", e);
        }
    }
}
