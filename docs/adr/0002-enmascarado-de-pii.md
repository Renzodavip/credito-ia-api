# ADR 0002: Enmascarar datos personales antes de llamar al LLM

**Estado:** aceptada

## Contexto
En un banco, los datos personales no pueden salir del perímetro hacia un proveedor externo sin anonimización.

## Decisión
`EnmascaradorPii` reemplaza email, CUIT/CUIL, CBU, DNI y teléfono por marcadores (`[DNI]`, `[EMAIL]`, etc.) antes de cualquier llamada al modelo. En la base de auditoría se guarda solo el texto enmascarado.

DNI y teléfono se detectan únicamente después de una palabra clave ("DNI", "cel", etc.) para no enmascarar montos como `20.000.000`.

## Consecuencias
- Hay un test de integración que verifica que el DNI nunca llega al cliente del LLM.
- No detecta nombres propios ni direcciones. En producción haría falta una solución DLP (por ejemplo, Microsoft Presidio o el DLP del proveedor cloud) o un modelo desplegado dentro del perímetro.
