# ADR 0004: El modelo copia el número y la unidad; el código convierte

**Estado:** aceptada

## Contexto
Probando el servicio con un modelo local chico (`llama3.2`, 3B) aparecieron dos problemas de extracción:

1. Con los campos del esquema como opcionales, el modelo devolvía solo la edad y omitía el resto, incluso con frases claras. Se resolvió haciendo todos los campos obligatorios y admitiendo `null` explícito (también dentro de los `enum`), para que no esté forzado a inventar un valor cuando el dato falta.
2. Pedirle al modelo que convirtiera ("montos en pesos, plazo en meses") producía errores de cuenta: "20 años" volvía como `plazoMeses: 20` en lugar de 240. Un error así cambia la decisión de crédito sin que ningún control lo note, porque 20 es un número válido.

Hacer cuentas y conversiones es justo lo que un LLM pequeño hace peor, y es lo que el código hace de forma exacta y testeable.

## Decisión
El modelo solo **copia** lo que dice el cliente; el código convierte.

- El esquema pide `ingresoValor` + `ingresoUnidad`, `montoValor` + `montoUnidad` (`PESOS`, `MILES`, `MILLONES`) y `plazoValor` + `plazoUnidad` (`ANIOS`, `MESES`). El valor es el número tal como aparece, sin convertir.
- `SalidaLlm` (lo que devuelve el modelo) se convierte a `DatosExtraidos` (pesos y meses) con `UnidadMonto` y `UnidadPlazo`. El resto del sistema (evaluador, auditoría, API) sigue trabajando con pesos y meses.
- Si el modelo informa un valor sin su unidad, no se adivina: se trata como salida inválida (reintento y, si persiste, 502).
- Bean Validation pone topes sobre el valor ya convertido: ingreso mensual entre 50.000 y 100.000.000, monto entre 1.000.000 y 5.000.000.000, plazo entre 12 y 480 meses.
- Los nombres de `datosFaltantes` se filtran en código contra la lista de campos válidos; no depende de que el modelo obedezca el prompt.

## Consecuencias
- La conversión es determinista y tiene tests unitarios. Cambiar de modelo no cambia las cuentas.
- Los topes frenan valores absurdos, incluido el número que un cliente intente inyectar en el texto (por ejemplo, "gano 99999999999" se rechaza por superar el tope del monto).
- **Límite conocido:** si el modelo elige mal la unidad pero el resultado cae dentro de los topes, el error no se detecta. Ejemplo observado con `llama3.2` y el esquema nuevo: la solicitud "Tengo 62 años, jubilado, cobro 1.200.000, quiero 40 millones a 20 años" quedó guardada con `plazoMeses: 20`; como `20 ANIOS` habría dado 240, el modelo tuvo que devolver `20 MESES`. (La salida cruda del modelo no se guarda, así que la unidad es una inferencia.) 20 meses es un plazo válido y pasa los topes. Esto se mitiga con un modelo mejor y se mide con un set de evaluación, no con más validaciones.
- Los topes son una elección razonable pero arbitraria; hay que revisarlos si cambian los productos de crédito.
- El esquema y el prompt son más largos, lo que suma tokens y algo de latencia.

## Alternativas consideradas
- **Dejar que el modelo convierta** (diseño anterior): descartada, por los errores descritos.
- **Usar un modelo más grande**: `llama3.1:8b` acertó la conversión en la prueba, pero en CPU tardó unos 129 s por solicitud. Ayuda, pero no elimina la causa: seguir delegando cuentas a un LLM.
