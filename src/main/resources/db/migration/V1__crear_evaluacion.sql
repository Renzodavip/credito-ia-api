CREATE TABLE evaluacion (
    id                     UUID PRIMARY KEY,
    idempotency_key        VARCHAR(100) UNIQUE,
    creado_en              TIMESTAMP WITH TIME ZONE NOT NULL,
    texto_enmascarado      TEXT         NOT NULL,
    datos_extraidos        TEXT,
    resultado              VARCHAR(30)  NOT NULL,
    cuota_estimada         NUMERIC(19, 2),
    relacion_cuota_ingreso NUMERIC(7, 4),
    motivos                TEXT,
    modelo                 VARCHAR(100) NOT NULL,
    intentos_llm           INT          NOT NULL,
    latencia_ms            BIGINT       NOT NULL
);

CREATE INDEX idx_evaluacion_creado_en ON evaluacion (creado_en);
