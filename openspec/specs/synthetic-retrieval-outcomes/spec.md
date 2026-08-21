# Especificación de `synthetic-retrieval-outcomes`

## Propósito

Producir resultados sintéticos deterministas separando cobertura, decisión e impedimento, sin habilitar conclusiones productivas.

## Requisitos

### Requirement: Límite exclusivamente sintético

La capacidad **DEBE (MUST)** aceptar solo fixtures y contribuciones declarados sintéticos y dirigirse a audiencia técnica. **NO DEBE (MUST NOT)** usar datos reales, emitir `UNEQUIVOCAL`, transferir documentos, ofrecer UI/API productiva ni formular afirmaciones factuales.

#### Scenario: Entrada no sintética

- GIVEN una entrada no declarada sintética o un adaptador real
- WHEN se intenta iniciar la recuperación
- THEN la operación se rechaza antes de evaluar contenido

#### Scenario: Selección aparentemente concluyente

- GIVEN un único candidato sintético destacado
- WHEN no hay ambigüedad observable
- THEN la decisión conservadora es `INSUFFICIENT`
- AND nunca se emite `UNEQUIVOCAL` ni una afirmación factual

### Requirement: Snapshot y cobertura inmutables

El sistema **DEBE (MUST)** congelar fuentes, gateways, obligatoriedad y configuración después de resolver el ámbito y antes del fan-out. `COMPLETE` **DEBE (MUST)** requerir contribución terminal válida de toda fuente esperada; cualquier ausencia **DEBE (MUST)** producir `PARTIAL` sin reducir el snapshot.

#### Scenario: Snapshot congelado

- GIVEN una operación con snapshot ya congelado
- WHEN cambian después la configuración o disponibilidad del fixture
- THEN la cobertura de esa operación se calcula contra el snapshot original

#### Scenario: Cobertura completa

- GIVEN contribuciones sintéticas esperadas
- WHEN todas completan válidamente
- THEN la cobertura es `COMPLETE`

#### Scenario: Cobertura parcial

- GIVEN contribuciones sintéticas esperadas
- WHEN cualquiera no completa válidamente
- THEN la cobertura es `PARTIAL`

### Requirement: Obligatoriedad e indisponibilidad

Una ausencia obligatoria **DEBE (MUST)** escalar con impedimento `UNAVAILABLE`, aunque existan candidatos. Una ausencia opcional **DEBE (MUST)** conservar `PARTIAL`, candidatos y decisión disponibles, junto con `UNAVAILABLE`, sin conclusión negativa.

#### Scenario: Gateway obligatorio ausente

- GIVEN un gateway obligatorio del snapshot sin contribución válida
- WHEN se consolida el resultado
- THEN se emiten cobertura `PARTIAL` e impedimento `UNAVAILABLE`
- AND el resultado escala aunque existan candidatos

#### Scenario: Gateway opcional ausente

- GIVEN que solo falta un gateway opcional
- WHEN existen candidatos autorizados
- THEN se conservan candidatos, decisión, `PARTIAL` y `UNAVAILABLE`
- AND no se afirma inexistencia ni cobertura completa

### Requirement: Decisiones conservadoras

La decisión **DEBE (MUST)** ser `AMBIGUOUS` para múltiples candidatos plausibles, `INSUFFICIENT` para evidencia categórica insuficiente, `STALE` para referencia o versión discrepante y `NOT_LOCATED_IN_SCOPE` únicamente con `COMPLETE` y cero candidatos. Esta última **NO DEBE (MUST NOT)** significar inexistencia universal.

#### Scenario: Candidatos ambiguos

- GIVEN múltiples candidatos plausibles
- WHEN se decide el resultado
- THEN resulta `AMBIGUOUS`

#### Scenario: Evidencia insuficiente

- GIVEN candidatos sin evidencia categórica suficiente
- WHEN se decide el resultado
- THEN resulta `INSUFFICIENT`

#### Scenario: Referencia o versión stale

- GIVEN una referencia o versión que no coincide con el snapshot evaluado
- WHEN se consolida el candidato
- THEN la decisión es `STALE` y exige revalidación

#### Scenario: No localizado dentro del ámbito

- GIVEN cobertura `COMPLETE` y ningún candidato
- WHEN se decide el resultado
- THEN resulta `NOT_LOCATED_IN_SCOPE`
- AND la salida limita expresamente la ausencia al ámbito autorizado consultado

### Requirement: Dimensiones y orden válidos

Cobertura, decisión e impedimento **DEBEN (MUST)** permanecer separados. `DENIED` **NO DEBE (MUST NOT)** incluir cobertura, decisión ni candidatos; `NOT_LOCATED_IN_SCOPE` **NO DEBE (MUST NOT)** coexistir con `PARTIAL`, candidatos o impedimento. Los candidatos **DEBEN (MUST)** conservar procedencia sintética autorizada y orden estable para la misma entrada y snapshot.

#### Scenario: Combinación inválida

- GIVEN una salida que combina `NOT_LOCATED_IN_SCOPE` con `PARTIAL` o candidatos
- WHEN se valida el resultado
- THEN la salida se rechaza como inválida

#### Scenario: Orden estable y cruce adversarial

- GIVEN las mismas contribuciones en distinto orden y una contribución de otro tenant
- WHEN se consolida el mismo snapshot
- THEN se obtiene idéntico orden de candidatos autorizados
- AND la contribución cruzada no afecta candidatos, conteos, decisión ni motivos
