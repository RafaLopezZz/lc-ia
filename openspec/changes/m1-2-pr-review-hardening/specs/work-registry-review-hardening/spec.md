# Especificación: endurecimiento de revisión del registro de trabajo M1.2

## Propósito

Definir el contrato verificable para corregir H1–H4 del registro en memoria con datos sintéticos, sin ampliar M1.2.

## Requisitos

### Requirement: Registro idempotente atómico por tenant y clave

El sistema MUST tratar registros equivalentes con el mismo `(tenant, idempotencyKey)` como una única operación, un único `Work` almacenado y una única secuencia de registro, incluso si llegan de forma concurrente. El sistema MUST conservar el orden determinista de registros distintos y el aislamiento entre tenants y gateways.

#### Scenario: H1 registros concurrentes equivalentes

- GIVEN dos operaciones concurrentes para el mismo tenant y la misma clave de idempotencia
- WHEN ambas completan el registro
- THEN ambas MUST observar la misma identidad de operación
- AND el registro MUST contener un solo `Work` y una sola secuencia para esa identidad

#### Scenario: H1 aislamiento de tenant

- GIVEN la misma clave de idempotencia para tenants distintos
- WHEN se registran las operaciones
- THEN el sistema MUST conservar identidades y secuencias independientes

### Requirement: Búsqueda de identidad almacenada

El sistema MUST proporcionar `findByIdempotency` acotado al tenant, que MAY devolver un `Optional` del `Work` almacenado aunque haya expirado. Esta búsqueda MUST representar identidad almacenada y MUST NOT afirmar elegibilidad de entrega.

#### Scenario: H2 identidad expirada distinta de pendiente elegible

- GIVEN un `Work` almacenado que ya expiró
- WHEN se consulta `findByIdempotency` con su tenant y clave
- THEN el resultado MUST contener ese `Work`
- AND una consulta de pendientes MUST NOT incluirlo como elegible

#### Scenario: H2 búsqueda aislada por tenant

- GIVEN una clave almacenada sólo para un tenant
- WHEN otro tenant consulta esa misma clave
- THEN el resultado MUST estar vacío

### Requirement: Estado efectivo derivado

El sistema MUST almacenar el ciclo de entrega como `DeliveryState` y MUST derivar `EffectiveStatus` únicamente de ese estado y de un instante de referencia. El sistema MUST NOT conservar un campo de estado efectivo competidor.

#### Scenario: H3 pendiente antes de expiración

- GIVEN un `Work` con `DeliveryState` almacenado `PENDING` y expiración posterior al instante de referencia
- WHEN se deriva su estado efectivo
- THEN `EffectiveStatus` MUST ser `PENDING`

#### Scenario: H3 pendiente expirada

- GIVEN un `Work` con `DeliveryState` almacenado `PENDING` y expiración no posterior al instante de referencia
- WHEN se deriva su estado efectivo
- THEN `EffectiveStatus` MUST ser `EXPIRED`
- AND el `DeliveryState` almacenado MUST permanecer `PENDING`

### Requirement: Snapshot temporal único en pendientes

Cada consulta de pendientes MUST usar un único instante de reloj capturado al inicio para todas sus comprobaciones de expiración. La elegibilidad MUST distinguir los trabajos almacenados de los pendientes efectivos.

#### Scenario: H4 reloj que avanza durante una consulta

- GIVEN varios trabajos cuya expiración rodea el instante inicial y un reloj que cambia en lecturas posteriores
- WHEN se ejecuta una sola consulta de pendientes
- THEN todos los trabajos MUST evaluarse contra el mismo instante inicial
- AND el resultado MUST NOT depender de lecturas posteriores del reloj

### Requirement: Evidencia de aceptación TDD por slice

Cada slice H1–H4 MUST seguir exactamente `acquire → test-only → focused run → preserve literal evidence → STOP before Green`. Si el test-only pasa, el equipo MUST clasificarlo como caracterización y preservar esa clasificación junto con la evidencia literal; MUST NOT continuar a Green en ese slice sin una decisión posterior explícita.

#### Scenario: Evidencia RED o caracterización

- GIVEN un slice H1, H2, H3 o H4 adquirido
- WHEN se añade sólo su prueba de aceptación y se ejecuta el alcance focalizado
- THEN se MUST preservar la salida literal y el resultado
- AND si pasa, MUST quedar clasificado como caracterización antes de detenerse

## Alcance y exclusiones preservados

- Se MUST conservar registro pre-poll, límite de confianza de validación sin crear `Work`, y el aislamiento tenant/gateway.
- La política ante duplicados con gateway, correlación o expiración conflictivos permanece indefinida y fuera de alcance.
- M1.1, M1.3+, límites de confianza, HTTP/API, persistencia, entrega, leases, ACKs, redelivery, reconexión, resultados y brokers están fuera de alcance.
- El predecesor `m1-2-remote-work-registration` MUST permanecer histórico `BLOCKED/maintainer_decision`; el intento nativo 15 MUST permanecer intacto.

## Contrato de resultado

Este cambio crea sólo la especificación sucesora. No modifica fuente ni tests, no ejecuta pruebas y no realiza commit, push, PR, GitHub ni acciones nativas.
