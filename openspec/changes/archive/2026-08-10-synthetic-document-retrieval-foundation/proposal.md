# Propuesta: base sintética de recuperación documental

## Intención y valor

Crear una vertical ejecutable para validar con datos sintéticos autorización, ámbito, cobertura, candidatos, abstención y aislamiento, hoy solo conceptuales.

## Alcance

### Incluido
- Núcleo, fixtures y simuladores en memoria para identidad, grants, fuentes, colecciones y gateways.
- Resolución determinista de `source scope` y `collection scope`, con snapshot de cobertura congelado antes del fan-out simulado.
- Candidatos sintéticos con orden estable para la misma entrada y snapshot.
- Dimensiones separadas: cobertura `COMPLETE`/`PARTIAL`; decisión `AMBIGUOUS`/`INSUFFICIENT`/`STALE`/`NOT_LOCATED_IN_SCOPE`; impedimento `DENIED`/`UNAVAILABLE`.
- Aclaración estructurada con descriptores seguros ante ámbitos autorizados equivalentes.
- Obligatoriedad, opcionalidad y aislamiento multi-tenant por construcción, nunca por filtrado final.
- Traza categórica minimizada, sin contenido sensible ni razonamiento libre.
- Escenarios reproducibles: cruces de tenant, grants revocados, referencias stale y gateways ausentes.

### No objetivos

Auth0, Spring Boot, PostgreSQL, gateway/red/long-polling real, filesystem, índice, OCR, parsers, LLM, UI/API, descarga, datos reales, afirmaciones factuales y `UNEQUIVOCAL`.

## Capacidades afectadas

### Nuevas capacidades
- `authorized-scope-resolution`: grants, aislamiento y aclaración determinista de ámbitos.
- `synthetic-retrieval-outcomes`: cobertura congelada, fan-out, candidatos y resultados multidimensionales.
- `minimized-retrieval-trace`: traza estructurada, categórica y segura de cada resultado.

### Capacidades modificadas

Ninguna; no existen especificaciones OpenSpec vigentes.

## Enfoque y decisiones

Una vertical ejecutable consumirá fixtures sintéticos, autorizará antes de buscar, congelará cobertura, simulará contribuciones y consolidará resultados deterministas. `COMPLETE` sin candidatos devolverá `NOT_LOCATED_IN_SCOPE`: no localizado en todas las fuentes autorizadas del ámbito consultado, nunca inexistencia universal. `UNEQUIVOCAL` seguirá bloqueado.

Confirmado: comportamiento, límites y audiencia técnica. Pendiente para diseño: toolchain, estructura interna y formato de fixtures.

## Áreas afectadas

| Área | Impacto |
|---|---|
| `openspec/changes/synthetic-document-retrieval-foundation/` | Artefactos SDD y escenarios |
| Futura unidad ejecutable | Núcleo y simuladores; ubicación pendiente |

## Riesgos

| Riesgo | Mitigación |
|---|---|
| Falsa seguridad | La simulación no prueba integración ni seguridad operativa |
| Fugas entre tenants | Autorizar y particionar desde el origen |
| Estados semánticos mezclados | Especificar dimensiones y combinaciones válidas por separado |
| Fixtures convertidos en política | Mantener valores sintéticos fuera de reglas productivas |

## Reversión

Retirar la unidad sintética y revertir el cambio; no habrá datos ni migraciones productivas.

## Criterios de éxito

- [ ] Misma entrada y snapshot producen idéntico ámbito, orden, resultado y traza.
- [ ] Los escenarios adversariales prueban aislamiento sin depender de filtrado final.
- [ ] Ausencias obligatorias y opcionales preservan cobertura, decisión e impedimento.
- [ ] `NOT_LOCATED_IN_SCOPE` solo aparece con cobertura `COMPLETE` y nunca afirma inexistencia universal.
- [ ] Ninguna salida revela contenido sensible, razonamiento libre ni entidades no autorizadas.

## Próximos artefactos

Especificaciones por capacidad, diseño técnico y tareas ejecutables compatibles con `auto-chain`.
