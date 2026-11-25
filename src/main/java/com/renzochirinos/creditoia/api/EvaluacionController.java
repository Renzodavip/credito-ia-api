package com.renzochirinos.creditoia.api;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evaluaciones")
public class EvaluacionController {

    private final EvaluacionService servicio;

    public EvaluacionController(EvaluacionService servicio) {
        this.servicio = servicio;
    }

    /**
     * 201 si se creó una evaluación nueva; 200 si la Idempotency-Key ya se había
     * usado y se devuelve la evaluación existente sin volver a llamar al LLM.
     */
    @PostMapping
    public ResponseEntity<EvaluacionResponse> crear(
            @Valid @RequestBody SolicitudRequest solicitud,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            UriComponentsBuilder uriBuilder) {

        EvaluacionService.EvaluacionGuardada guardada = servicio.evaluar(solicitud.texto(), idempotencyKey);
        if (guardada.reutilizada()) {
            return ResponseEntity.ok(guardada.respuesta());
        }
        URI ubicacion = uriBuilder.path("/api/v1/evaluaciones/{id}")
                .buildAndExpand(guardada.respuesta().id())
                .toUri();
        return ResponseEntity.created(ubicacion).body(guardada.respuesta());
    }

    @GetMapping("/{id}")
    public EvaluacionResponse obtener(@PathVariable UUID id) {
        return servicio.obtener(id);
    }

    @PostMapping("/lote")
    public List<ResultadoLote> evaluarLote(@Valid @RequestBody LoteRequest lote) {
        return servicio.evaluarLote(lote.solicitudes());
    }
}
