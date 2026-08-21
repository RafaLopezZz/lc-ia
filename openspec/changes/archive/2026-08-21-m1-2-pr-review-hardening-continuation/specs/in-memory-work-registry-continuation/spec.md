# Especificación: continuación del registro de trabajo en memoria

## Propósito

Consolidar el modelo de estado observable y garantizar una evaluación temporal coherente para consultas de trabajo pendiente, usando exclusivamente datos sintéticos.

## Requirements

### Requirement: Modelo de estado efectivo consolidado

El sistema MUST conservar `DeliveryState` como el estado de ciclo de vida almacenado, incluido `DeliveryState.PENDING`. El sistema MUST exponer el estado observable mediante `EffectiveStatus`, derivado de `DeliveryState` y de un instante explícito. Tras migrar todos los consumidores legítimos, el sistema MUST NOT conservar `Status`, `statusFor`, ni `Work.status`.

#### Coverage classification: H3-R GREEN BASELINE

La cobertura base MUST estar GREEN antes del refactor y MUST incluir los escenarios de trabajo pendiente no expirado, trabajo pendiente expirado y consumidor legítimo migrado. La consulta por correlación MUST permanecer aislada por tenant. Esta clasificación prueba el comportamiento existente; H3-R MUST NOT crear ni ejecutar una fase RED.

#### Coverage classification: H3-R REFACTOR TARGET

El inventario de refactor MUST incluir `Status`, `statusFor`, `Work.status` y cada consumidor package-local legítimo. El refactor MUST retirar ese inventario sin cambiar el comportamiento clasificado como GREEN BASELINE.

#### Coverage classification: H3-R GREEN REGRESSION

La regresión MUST estar GREEN después del refactor y MUST volver a cubrir todo el inventario GREEN BASELINE: `EffectiveStatus.PENDING` antes del vencimiento, `EffectiveStatus.EXPIRED` en o después del vencimiento, conservación de `DeliveryState.PENDING`, migración del consumidor legítimo y aislamiento por tenant. Además, MUST confirmar que la superficie retirada no permanece.

#### Coverage classification: Excepción H3 del predecesor

H3 has a technical PASS and a documented native-settlement exception. This continuation neither inherits nor resets that exception or any predecessor/native state.

#### Scenario: Trabajo pendiente no expirado

- GIVEN un trabajo con `DeliveryState.PENDING` cuyo vencimiento es posterior al instante evaluado
- WHEN un consumidor obtiene su estado efectivo
- THEN el resultado es `EffectiveStatus.PENDING`
- AND el estado almacenado permanece `DeliveryState.PENDING`

#### Scenario: Consumidor legítimo migrado

- GIVEN un consumidor que antes obtenía el estado mediante la superficie retirada
- WHEN consulta el estado de un trabajo después de la migración
- THEN obtiene el resultado mediante `EffectiveStatus`
- AND no depende de `Status`, `statusFor` ni `Work.status`

#### Scenario: Trabajo pendiente expirado

- GIVEN un trabajo con `DeliveryState.PENDING` cuyo vencimiento es igual o anterior al instante evaluado
- WHEN un consumidor obtiene su estado efectivo
- THEN el resultado es `EffectiveStatus.EXPIRED`
- AND el estado almacenado permanece `DeliveryState.PENDING`

#### Scenario: H3-R nunca inicia RED

- GIVEN el inventario H3-R clasificado
- WHEN se planifica la migración de la superficie heredada
- THEN la secuencia es GREEN BASELINE, REFACTOR y GREEN REGRESSION
- AND no se crea ni se ejecuta una fase RED para H3-R

### Requirement: Instantánea temporal única por consulta de gateway

El sistema MUST evaluar todos los candidatos de cada consulta `pendingFor(TenantId, GatewayId)` contra un único instante capturado al inicio de esa consulta. El sistema MUST NOT variar el instante de evaluación entre candidatos de la misma consulta.

#### Scenario: Candidatos evaluados con la misma instantánea

- GIVEN una consulta de trabajos pendientes para un tenant y gateway con varios candidatos
- WHEN la consulta evalúa el estado efectivo de los candidatos
- THEN todos se evalúan contra el mismo instante capturado
- AND devuelve solo los candidatos con estado efectivo pendiente en ese instante

#### Scenario: Límite de vencimiento durante la consulta

- GIVEN dos candidatos cuyo vencimiento queda a lados distintos del instante capturado
- WHEN la consulta se ejecuta aunque avance el reloj durante su recorrido
- THEN su inclusión se decide respecto al instante capturado
- AND ningún candidato se reevalúa con un instante posterior
