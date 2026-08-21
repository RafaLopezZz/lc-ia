# Diseño: continuación del endurecimiento de revisión M1.2

## Enfoque técnico

Este cambio futuro consolida la lectura de estado de `InMemoryWorkRegistry` sin alterar H1/H2 ni el registro nativo del predecesor. H3-R usa exclusivamente GREEN BASELINE → REFACTOR → GREEN REGRESSION; nunca es RED. Elimina la representación paralela `Status`; `DeliveryState` sigue siendo el ciclo de vida almacenado y `EffectiveStatus` es la única lectura derivada. H4 toma un `Instant` antes de recorrer una consulta `pendingFor` y lo reutiliza para todos sus candidatos.

## Decisiones de arquitectura

| Decisión | Alternativa descartada | Razón |
|---|---|---|
| Eliminar `Status`, `statusFor` y `Work.status` en una migración atómica. | Adaptador deprecado de `Status`. | El adaptador mantiene exactamente el vocabulario duplicado que H3-R debe retirar. Los consumidores actuales son package-local. |
| Conservar `DeliveryState` en `Work` y calcular `EffectiveStatus` mediante el helper que recibe `Work` e `Instant`. | Guardar `EXPIRED` en el registro. | La expiración depende del tiempo de lectura; persistirla puede divergir del reloj y no aporta valor con el único estado de ciclo de vida actual. |
| Capturar `now` al inicio de `pendingFor` y pasarlo a cada evaluación. | Leer `clock.instant()` dentro del filtro. | Un reloj que avanza durante el stream no puede cambiar la inclusión entre candidatos de la misma consulta. |
| Limitar cada futuro acquire TDD a 750 líneas cambiadas como máximo. | Un acquire combinado sin límite. | H3-R y H4 son slices independientes y el límite preserva revisión y rollback focalizados. |

## Flujo de datos

    Registration → Work(DeliveryState.PENDING, expiresAt)
                         │
    effectiveStatusFor(work, now) → EffectiveStatus
                         │
    pendingFor(tenant, gateway): now = clock.instant()
                         └→ filtra tenant/gateway/DeliveryState y EffectiveStatus con el mismo now

La derivación trata el vencimiento igual a `now` como `EXPIRED`; el `DeliveryState` almacenado no cambia. La consulta por correlación migra de `statusFor` a `effectiveStatusFor(tenant, correlation)` y conserva su aislamiento por tenant.

## Cambios de archivos

| Archivo | Acción futura | Descripción |
|---|---|---|
| `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/InMemoryWorkRegistry.java` | Modificar | Retirar la superficie `Status`; construir `Work` solo con `DeliveryState`; reutilizar la derivación de estado con un instante capturado por consulta. |
| `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/InMemoryWorkRegistryTest.java` | Modificar | Migrar los consumidores de la superficie retirada y caracterizar el snapshot único de H4. |
| `openspec/changes/m1-2-pr-review-hardening-continuation/design.md` | Crear | Registrar este diseño independiente. |

## Interfaces / contratos

- `Work` conserva `deliveryState` y elimina el componente `status`.
- `Status` y `statusFor(TenantId, CorrelationId)` dejan de existir.
- `EffectiveStatus` sigue siendo `PENDING | EXPIRED`; su cálculo usa `DeliveryState`, `expiresAt` y un `Instant` de evaluación.
- `pendingFor(TenantId, GatewayId)` conserva firma, orden y filtro tenant/gateway; para una invocación usa un único instante.
- `effectiveStatusFor(TenantId, CorrelationId)` es el reemplazo de lectura por correlación y mantiene el scope de tenant.

## Estrategia de pruebas

| Clasificación | Inventario | Cobertura obligatoria |
|---|---|---|
| H3-R GREEN BASELINE | Comportamiento observable existente. | No expirado → `PENDING`; expirado/en el límite → `EXPIRED`; `DeliveryState.PENDING` no cambia; consulta de correlación mantiene el tenant. |
| H3-R REFACTOR TARGET | `Status`, `statusFor`, `Work.status` y consumidores package-local. | Retiro y migración atómica sin alterar el inventario baseline. |
| H3-R GREEN REGRESSION | Inventario baseline tras el retiro. | Repite íntegramente la cobertura baseline y confirma ausencia de la superficie retirada. |
| H4 RED | Lectura de reloj durante una sola consulta. | Un reloj de secuencia evidencia múltiples lecturas antes de la corrección. |
| H4 GREEN REGRESSION | Snapshot único de gateway. | Múltiples candidatos se incluyen o excluyen respecto al mismo instante, aunque avance el reloj. |

H3-R se ejecuta por fases GREEN BASELINE → REFACTOR → GREEN REGRESSION, sin fase RED. H4 conserva su ciclo independiente RED → GREEN → REFACTOR. Este diseño no ejecuta ni crea pruebas.

## Matriz de amenazas

N/A — no se modifica routing, shell, subprocess, automatización VCS/PR, clasificación de ejecutables ni integración de procesos.

## Migración / rollout

No hay migración de datos ni flag. H3-R actualiza todos los consumidores package-local en el mismo cambio que retira el constructor/componente heredado. H4 se aplica después o como slice autónomo posterior; ambos cambios se revierten individualmente y no modifican ni reinician artefactos del predecesor.

## Contexto del predecesor preservado

H3 has a technical PASS and a documented native-settlement exception. This continuation neither inherits nor resets that exception or any predecessor/native state.

## Preguntas abiertas

Ninguna.
