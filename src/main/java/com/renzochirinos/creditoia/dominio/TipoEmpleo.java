package com.renzochirinos.creditoia.dominio;

import com.fasterxml.jackson.annotation.JsonEnumDefaultValue;

public enum TipoEmpleo {
    RELACION_DEPENDENCIA,
    AUTONOMO,
    MONOTRIBUTISTA,
    JUBILADO,
    @JsonEnumDefaultValue
    DESCONOCIDO
}
