# Propuesta: Hardening de revisión de PR M1.2

**Contexto confirmado:** Jira `LCIA-23`; rama `feat/m1-2-in-memory-work-queue`; PR #5. Este es un sucesor independiente. El predecesor `m1-2-remote-work-registration` permanece histórico **BLOCKED/maintainer_decision**; el intento nativo 15 permanece intacto.

## Intención

Corregir cuatro hallazgos de revisión en el registro en memoria sin ampliar el comportamiento M1.2: eliminar la carrera de idempotencia, distinguir identidad de entrega, unificar el estado efectivo y hacer consistente el tiempo de cada consulta pendiente.

## Alcance

### Incluido
- **H1:** hacer atómico el registro por `(tenant, idempotencyKey)` para conservar una única identidad de operación ante registros equivalentes concurrentes.
- **H2:** sustituir el overload orientado a tests por `findByIdempotency`, acotado al tenant e incluyendo el registro almacenado aunque esté expirado.
- **H3:** almacenar el ciclo de entrega como `DeliveryState` y derivar `EffectiveStatus` desde ese estado y un instante del reloj; no conservar un campo de estado competidor.
- **H4:** capturar un único `Clock.instant()` al inicio de cada consulta pendiente y reutilizarlo en todas sus comprobaciones de expiración.
- Conservar aislamiento tenant/gateway, orden de registro determinista, registro pre-poll y límite de confianza de validación sin creación de `Work`.

### Fuera de alcance
- La política para una clave duplicada con gateway, correlación o expiración conflictivos queda **indefinida y fuera de alcance**; no se infiere reutilización ni rechazo.
- M1.1, M1.3+, `SyntheticTrustBoundary`, adaptador HTTP, cableado HTTP/API, long polling, persistencia, entrega HTTP/API, leases, ACKs, redelivery, reconexión, resultados, brokers y cambios al intento nativo o estado del predecesor.

## Capacidades

### Nuevas capacidades
- `work-registry-review-hardening`: contrato de atomicidad, identidad, estado efectivo y snapshot temporal del registro en memoria.

### Capacidades modificadas
Ninguna; no existen especificaciones base aplicables.

## Enfoque

**Propuesto:** sincronizar sólo la sección crítica de `register` con primitivas JDK, manteniendo el almacenamiento ordenado existente. Añadir la búsqueda explícita y derivar el estado efectivo desde `DeliveryState` y el instante ya capturado. Es la solución atómica más pequeña; no añade índices ni dependencias.

## Áreas afectadas

| Área | Impacto | Descripción |
|---|---|---|
| `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/InMemoryWorkRegistry.java` | Modificado | H1–H4. |
| `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java` | Modificado | Caracterización de H1–H4. |

## Riesgos

| Riesgo | Probabilidad | Mitigación |
|---|---|---|
| Migración de tipos de estado rompe consumidores package-local | Media | Actualizar sólo consumidores de H3. |
| Bloqueo reduce throughput | Baja | Es un registro en memoria; medir antes de rediseñar. |

## Plan de reversión

Revertir el cambio de este sucesor; no requiere migración, datos persistidos ni cambios al predecesor.

## Dependencias

- Ninguna; sólo JDK.

## Criterios de éxito

- [ ] H1–H4 están especificados y verificados con datos sintéticos.
- [ ] Registros equivalentes concurrentes conservan una sola identidad y orden determinista.
- [ ] La búsqueda de identidad ve registros expirados sin habilitar su entrega.
- [ ] Cada consulta pendiente usa un único instante y el estado efectivo no diverge del ciclo de entrega.

## Contrato de resultado

Esta propuesta crea únicamente el artefacto sucesor. No modifica fuente ni tests, no ejecuta tests, ni realiza commit, push, PR, GitHub o acciones nativas. Conserva el predecesor bloqueado y el intento nativo 15 intactos.
