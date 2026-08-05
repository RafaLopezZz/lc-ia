# Tareas: base sintética de recuperación documental

## Review Workload Forecast

Líneas modificadas estimadas: 900–1.200
Riesgo: Alto — 30 escenarios e invariantes de seguridad
delivery_strategy: `auto-chain`
review_budget_lines: 800
chain_strategy: `stacked-to-main`
Estado: decisión resuelta; aprobada por el usuario después del forecast.

Decision needed before apply: No
Chained PRs recommended: Yes
Chain strategy: stacked-to-main
400-line budget risk: High
800-line budget risk: High

### Unidades revisables

| Unidad | Alcance/base                           | Test focal                                                                      | Harness            | Reversión                         |
| ------ | -------------------------------------- | ------------------------------------------------------------------------------- | ------------------ | --------------------------------- |
| 1      | Runner, guard y modelo/`main`          | `mvn -f synthetic-retrieval/pom.xml -Dtest=SyntheticRetrievalScenarioTest test` | JUnit guard/modelo | `pom.xml`, modelo, guard, pruebas |
| 2      | Autorización y scope/unidad 1          | `mvn -f synthetic-retrieval/pom.xml -Dtest=SyntheticRetrievalScenarioTest test` | JUnit scope        | resolución y pruebas              |
| 3      | Simulación, resultado y traza/unidad 2 | `mvn -f synthetic-retrieval/pom.xml test`                                       | suite JUnit        | simulador, consolidación, pruebas |

## Fase 1: Runner, barrera y modelo — Unidad 1

- [x] 1.1 Crear `synthetic-retrieval/pom.xml` con Java 25, JUnit Jupiter 5.13.4 y Surefire 3.5.3; añadir smoke check y ejecutar `mvn test`.
- [x] 1.2 Comprobar primero el rechazo de procedencia/simulador no sintéticos; implementar `SyntheticOnlyGuard` en `SyntheticRetrieval.java`. Mapea `Límite exclusivamente sintético / Entrada no sintética`.
- [x] 1.3 Comprobar e implementar en `RetrievalModel.java` IDs opacos, copias inmutables ordenadas, dimensiones y constructores inválidos; excluir `UNEQUIVOCAL`. Mapea `Dimensiones y orden / Combinación inválida` y `Determinismo e integridad / Estado semántico inválido`.

## Fase 2: Autorización y ámbito — Unidad 2

- [x] 2.1 Comprobar e implementar partición `TenantId`, contexto, memberships, grants y elegibilidad source/collection. Mapea todos los escenarios de `Autoridad previa` y `Elegibilidad de tipos de ámbito`.
- [x] 2.2 Comprobar e implementar ámbito mínimo, aclaración segura y repetición. Mapea todos los escenarios de `Selección y aclaración deterministas` y `Traza / Aclaración de ámbito`.
- [x] 2.3 Comparar fixtures con/sin entidad cruzada y exigir salidas/trazas idénticas; comprobar denegación mínima. Mapea `Aislamiento por construcción / Cruce adversarial` y `Aislamiento observable / Denegación minimizada`.

## Fase 3: Snapshot y consolidación — Unidad 3

- [x] 3.1 Comprobar antes de implementar snapshot defensivo y cobertura ante mutación posterior. Mapea todos los escenarios de `Snapshot y cobertura inmutables`.
- [x] 3.2 Comprobar ausencias obligatorias/opcionales con `PARTIAL`, `UNAVAILABLE` y candidatos. Implementar ambos escenarios de `Obligatoriedad e indisponibilidad`.
- [x] 3.3 Comprobar e implementar `AMBIGUOUS`, `INSUFFICIENT`, `STALE` y `NOT_LOCATED_IN_SCOPE` solo con `COMPLETE` y cero candidatos. Mapea `Selección aparentemente concluyente` y todos los escenarios de `Decisiones conservadoras`.
- [x] 3.4 Parametrizar permutaciones y contribución cross-tenant para orden estable e igualdad. Mapea `Dimensiones y orden / Orden estable y cruce adversarial`.

## Fase 4: Traza minimizada y cierre — Unidad 3

- [x] 4.1 Comprobar e implementar `MinimizedTrace` por lista permitida, sin texto libre ni campos sensibles. Mapea `Traza estructurada`, `Minimización` y `Aislamiento observable / Contribución de otro tenant`.
- [x] 4.2 Comprobar traza determinista y rechazo de estados incompatibles. Mapea ambos escenarios de `Determinismo e integridad semántica`; ejecutar `mvn -f synthetic-retrieval/pom.xml test`.
- [x] 4.3 Verificar solo cinco archivos, fixtures internos y ausencia de Spring, PostgreSQL, Auth0, red, filesystem, OCR, LLM, UI/API y datos reales. Mapea `Límite exclusivamente sintético` y los no objetivos.

## Fase 5. Runtime compliance remediation

- [x] 5.1 Implement the connected end-to-end retrieval operation required by the uncovered scenarios.
- [x] 5.2 Add runtime compliance for clarification traces.
- [x] 5.3 Add runtime compliance for denial traces, preserving the DENIED information-minimization contract.
- [x] 5.4 Enforce Gateway.required during gateway consolidation.
- [x] 5.5 Add scenario-level tests for all 10 previously uncovered scenarios.
- [x] 5.6 Run the complete Maven test and package verification.
