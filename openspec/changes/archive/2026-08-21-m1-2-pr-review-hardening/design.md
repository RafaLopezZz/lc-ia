# Diseño: endurecimiento del registro M1.2

## Enfoque técnico

Aplicar H1–H4 únicamente al registro en memoria, con primitivas JDK y sin nuevos índices, adaptadores ni cableado. La lista ordenada existente sigue siendo la fuente de identidad y orden; una sección crítica de `register` hace indivisible buscar, asignar secuencia e insertar. La identidad almacenada, el estado efectivo y la elegibilidad de entrega quedan explícitamente separados.

## Decisiones de arquitectura

| Decisión | Alternativas descartadas | Justificación |
|---|---|---|
| Sincronizar sólo el tramo buscar→asignar secuencia→insertar de `register` | `ConcurrentHashMap`, doble comprobación, bloqueo global de todas las lecturas | Es el límite mínimo que evita dos identidades/secuencias para la misma clave y conserva el orden de inserción de la lista. |
| Exponer `Optional<Work> findByIdempotency(TenantId, IdempotencyKey)` | Reutilizar el overload de prueba, filtrar por expiración | La búsqueda representa identidad almacenada, no entrega; debe devolver trabajo expirado y permanecer aislada por tenant. |
| Persistir `DeliveryState`; calcular `EffectiveStatus` | Campo `status` mutable/duplicado | Una única fuente de estado de ciclo evita divergencia. `PENDING` y expiración no posterior a la referencia produce `EXPIRED` sin mutar el estado almacenado. |
| Capturar `Instant now = clock.instant()` una vez por consulta pendiente | Consultar el reloj por elemento | Todas las expiraciones se evalúan contra el mismo snapshot temporal. |

No se definen políticas para duplicados conflictivos ni abstracciones de M1.3 (índices, repositorios, leases, ACKs o redelivery). HTTP/API y `SyntheticTrustBoundary` no cambian.

## Flujo de datos

```text
register(request)
  └─ synchronized registry: find tenant+key → existente | allocate sequence → append Work

findByIdempotency(tenant, key) ──→ Optional<stored Work> (sin reloj)
pendingFor(tenant, gateway) ──→ now = clock.instant() una vez
                              └─ DeliveryState + expiresAt + now → EffectiveStatus → elegible
```

El orden sólo lo determina el `append` dentro de la sección crítica. Las lecturas pendientes seleccionan tenant y gateway, derivan el estado efectivo contra `now`, y sólo incluyen `PENDING` efectivo. La búsqueda de identidad no participa en esta selección.

## Contratos e invariantes

```java
Optional<Work> findByIdempotency(TenantId tenantId, IdempotencyKey idempotencyKey);
// Work stores DeliveryState; EffectiveStatus is derived from (DeliveryState, expiresAt, Instant).
```

- Para el mismo `(tenant, idempotencyKey)`, registros equivalentes concurrentes observan la misma operación, un único `Work` y una única secuencia.
- La misma clave en otro tenant no coincide.
- `findByIdempotency` no lee el reloj ni promete entrega.
- `DeliveryState` no se modifica al derivar `EXPIRED`; `EffectiveStatus` no se almacena como campo competidor.
- Cada llamada pendiente crea exactamente un `now` local antes de evaluar trabajos.

## Cambios de archivos

| Archivo | Acción | Descripción |
|---|---|---|
| `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/InMemoryWorkRegistry.java` | Modificar | Límites H1–H4 y contratos de estado/búsqueda. |
| `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java` | Modificar | Cuatro aceptaciones aisladas y regresiones directas futuras. |

## Plan de pruebas: cuatro slices red-first

Cada slice se adquiere y ejecuta de forma aislada: `acquire → test-only → focused run → preservar salida literal → STOP before Green`. Si pasa, se registra como caracterización; no se implementa ni se inicia el siguiente Green.

| Slice | Prueba de aceptación RED | Límite de implementación posterior | Regresión directa planificada |
|---|---|---|---|
| H1 | Dos registros concurrentes equivalentes devuelven una identidad; sólo hay un trabajo y una secuencia. | Sección sincronizada completa en `register`. | Orden de claves distintas y aislamiento entre tenants. |
| H2 | Un trabajo expirado se encuentra por tenant+clave, pero no figura pendiente; otro tenant recibe vacío. | Añadir `findByIdempotency`; retirar el overload orientado a pruebas. | Pre-poll y selección exacta de gateway. |
| H3 | Antes de expirar deriva `PENDING`; al límite o después deriva `EXPIRED` sin mutar `DeliveryState`. | Reemplazar estado competidor por estado almacenado y derivación. | Estado de correlación/entrega existente. |
| H4 | Un reloj que cambia tras su primera lectura no altera la decisión entre varios trabajos. | Un único `now` al inicio de cada consulta pendiente. | Exclusión de expiración exacta y filtro tenant/gateway. |

Las regresiones directas se añaden o ejecutan sólo cuando una autorización posterior permita Green/validación; en esta fase no se modifica ni ejecuta ninguna prueba.

## Matriz de amenazas

N/A — no hay routing, shell, subprocess, VCS/PR automation, clasificación de ejecutables ni integración de procesos.

## Migración, rollout y preguntas

No requiere migración, flag ni rollout: el almacenamiento es en memoria y el revertido es revertir este sucesor. No hay preguntas abiertas que bloqueen el diseño.

## Contrato de resultado

Este diseño persiste el artefacto sucesor en ambos stores. No modifica fuente ni pruebas, no ejecuta pruebas, y no realiza commit, push, PR, GitHub ni acciones nativas. El predecesor bloqueado y el intento nativo 15 permanecen intactos.
