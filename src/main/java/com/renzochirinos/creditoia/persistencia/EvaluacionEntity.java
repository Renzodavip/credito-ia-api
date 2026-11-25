package com.renzochirinos.creditoia.persistencia;

import com.renzochirinos.creditoia.dominio.ResultadoEvaluacion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Registro de auditoría de cada evaluación: qué entró (enmascarado), qué devolvió
 * el modelo, qué se decidió y con qué modelo. Nunca se guarda el texto original.
 */
@Entity
@Table(name = "evaluacion")
public class EvaluacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "texto_enmascarado", nullable = false, columnDefinition = "text")
    private String textoEnmascarado;

    @Column(name = "datos_extraidos", columnDefinition = "text")
    private String datosExtraidosJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ResultadoEvaluacion resultado;

    @Column(name = "cuota_estimada", precision = 19, scale = 2)
    private BigDecimal cuotaEstimada;

    @Column(name = "relacion_cuota_ingreso", precision = 7, scale = 4)
    private BigDecimal relacionCuotaIngreso;

    @Column(columnDefinition = "text")
    private String motivos;

    @Column(nullable = false, length = 100)
    private String modelo;

    @Column(name = "intentos_llm", nullable = false)
    private int intentosLlm;

    @Column(name = "latencia_ms", nullable = false)
    private long latenciaMs;

    protected EvaluacionEntity() {
        // requerido por JPA
    }

    public EvaluacionEntity(String idempotencyKey, Instant creadoEn, String textoEnmascarado,
                            String datosExtraidosJson, ResultadoEvaluacion resultado,
                            BigDecimal cuotaEstimada, BigDecimal relacionCuotaIngreso, String motivos,
                            String modelo, int intentosLlm, long latenciaMs) {
        this.idempotencyKey = idempotencyKey;
        this.creadoEn = creadoEn;
        this.textoEnmascarado = textoEnmascarado;
        this.datosExtraidosJson = datosExtraidosJson;
        this.resultado = resultado;
        this.cuotaEstimada = cuotaEstimada;
        this.relacionCuotaIngreso = relacionCuotaIngreso;
        this.motivos = motivos;
        this.modelo = modelo;
        this.intentosLlm = intentosLlm;
        this.latenciaMs = latenciaMs;
    }

    public UUID getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public String getTextoEnmascarado() {
        return textoEnmascarado;
    }

    public String getDatosExtraidosJson() {
        return datosExtraidosJson;
    }

    public ResultadoEvaluacion getResultado() {
        return resultado;
    }

    public BigDecimal getCuotaEstimada() {
        return cuotaEstimada;
    }

    public BigDecimal getRelacionCuotaIngreso() {
        return relacionCuotaIngreso;
    }

    public String getMotivos() {
        return motivos;
    }

    public String getModelo() {
        return modelo;
    }

    public int getIntentosLlm() {
        return intentosLlm;
    }

    public long getLatenciaMs() {
        return latenciaMs;
    }
}
