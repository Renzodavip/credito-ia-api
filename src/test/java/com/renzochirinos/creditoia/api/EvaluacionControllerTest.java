package com.renzochirinos.creditoia.api;

import com.renzochirinos.creditoia.dominio.ResultadoEvaluacion;
import com.renzochirinos.creditoia.ia.LlmNoDisponibleException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EvaluacionController.class)
class EvaluacionControllerTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private EvaluacionService servicio;

    @Test
    void crearDevuelve201ConLocation() throws Exception {
        when(servicio.evaluar(anyString(), isNull()))
                .thenReturn(new EvaluacionService.EvaluacionGuardada(respuesta(), false));

        mvc.perform(post("/api/v1/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Tengo 35 años y quiero un crédito\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/v1/evaluaciones/" + ID)))
                .andExpect(jsonPath("$.resultado").value("PRECALIFICA"));
    }

    @Test
    void mismaIdempotencyKeyDevuelve200() throws Exception {
        when(servicio.evaluar(anyString(), eq("clave-1")))
                .thenReturn(new EvaluacionService.EvaluacionGuardada(respuesta(), true));

        mvc.perform(post("/api/v1/evaluaciones")
                        .header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Tengo 35 años\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void textoVacioDevuelve400ConProblemDetail() throws Exception {
        mvc.perform(post("/api/v1/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void evaluacionInexistenteDevuelve404() throws Exception {
        when(servicio.obtener(ID)).thenThrow(new EvaluacionNoEncontradaException(ID));

        mvc.perform(get("/api/v1/evaluaciones/" + ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Evaluación no encontrada"));
    }

    @Test
    void llmCaidoDevuelve503SinFiltrarDetallesInternos() throws Exception {
        when(servicio.evaluar(anyString(), isNull()))
                .thenThrow(new LlmNoDisponibleException("timeout contra api interna"));

        mvc.perform(post("/api/v1/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"hola\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("api interna"))));
    }

    private static EvaluacionResponse respuesta() {
        return new EvaluacionResponse(ID, ResultadoEvaluacion.PRECALIFICA, new BigDecimal("550000.00"),
                new BigDecimal("0.1833"), List.of(), null, "test/modelo", Instant.parse("2026-11-25T12:00:00Z"));
    }
}
