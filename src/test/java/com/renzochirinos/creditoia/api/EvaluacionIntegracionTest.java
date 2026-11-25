package com.renzochirinos.creditoia.api;

import com.renzochirinos.creditoia.ia.LlmClient;
import com.renzochirinos.creditoia.persistencia.EvaluacionEntity;
import com.renzochirinos.creditoia.persistencia.EvaluacionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de punta a punta con PostgreSQL real (Testcontainers) y Flyway.
 * Solo el LLM está simulado: no queremos depender de un servicio externo en CI.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class EvaluacionIntegracionTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String JSON_LLM = """
            {"edad":35,"ingresoValor":3000000,"ingresoUnidad":"PESOS",
             "montoValor":50,"montoUnidad":"MILLONES","plazoValor":20,"plazoUnidad":"ANIOS",
             "tipoEmpleo":"RELACION_DEPENDENCIA","datosFaltantes":[]}
            """;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private EvaluacionRepository repositorio;

    @MockitoBean
    private LlmClient llm;

    @BeforeEach
    void setUp() {
        repositorio.deleteAll();
        when(llm.completarJson(anyString(), anyString(), anyMap())).thenReturn(JSON_LLM);
        when(llm.modelo()).thenReturn("test/modelo");
    }

    @Test
    void evaluaGuardaAuditoriaYNoEnviaPiiAlModelo() throws Exception {
        mvc.perform(post("/api/v1/evaluaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"texto":"Tengo 35 años, DNI 30.123.456, cobro 3.000.000 y quiero 50 millones a 20 años"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultado").value("PRECALIFICA"));

        ArgumentCaptor<String> enviado = ArgumentCaptor.forClass(String.class);
        verify(llm).completarJson(anyString(), enviado.capture(), anyMap());
        assertThat(enviado.getValue()).contains("[DNI]").doesNotContain("30.123.456");

        List<EvaluacionEntity> guardadas = repositorio.findAll();
        assertThat(guardadas).hasSize(1);
        assertThat(guardadas.getFirst().getTextoEnmascarado()).doesNotContain("30.123.456");
        assertThat(guardadas.getFirst().getModelo()).isEqualTo("test/modelo");
    }

    @Test
    void mismaIdempotencyKeyNoVuelveALlamarAlModelo() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(post("/api/v1/evaluaciones")
                            .header("Idempotency-Key", "solicitud-123")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"texto\":\"Tengo 35 años\"}"))
                    .andExpect(i == 0 ? status().isCreated() : status().isOk());
        }

        verify(llm, times(1)).completarJson(anyString(), anyString(), anyMap());
        assertThat(repositorio.count()).isEqualTo(1);
    }

    @Test
    void loteProcesaTodasLasSolicitudesEnOrden() throws Exception {
        mvc.perform(post("/api/v1/evaluaciones/lote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solicitudes\":[\"uno\",\"dos\",\"tres\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].indice").value(0))
                .andExpect(jsonPath("$[2].indice").value(2))
                .andExpect(jsonPath("$[1].evaluacion.resultado").value("PRECALIFICA"));

        verify(llm, atLeastOnce()).completarJson(anyString(), anyString(), anyMap());
        assertThat(repositorio.count()).isEqualTo(3);
    }
}
