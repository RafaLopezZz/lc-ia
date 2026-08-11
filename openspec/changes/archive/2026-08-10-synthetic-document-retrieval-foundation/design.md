# Diseño: base sintética de recuperación documental

## Enfoque y evidencia

Se implementará una sola unidad Java de dominio, ejecutable únicamente con fixtures y simuladores en memoria. El flujo autoriza y acota por tenant antes de resolver ámbito, congela un snapshot, simula contribuciones y consolida un resultado y una traza deterministas. No crea integración remota/local ni contrato productivo.

Evidencia read-only del 2026-08-02: `java --version` y `javac --version` informan Microsoft OpenJDK 25.0.1 LTS; `mvn --version`, Maven 3.9.6 sobre ese JDK; `gradle` no está disponible. El repositorio no contiene implementación, manifiesto ni runner. Se elige Maven, Java 25, JUnit Jupiter 5.13.4 y Surefire 3.5.3, ejecutados con `mvn test`; la documentación vigente de JUnit confirma esa combinación. El aviso Jansi de Maven bajo JDK 25 no impide la ejecución.

## Decisiones

| Tema | Elección y razón | Descartado |
|---|---|---|
| Toolchain | Maven/JDK 25 y una dependencia de test; es la capacidad instalada mínima con runner estándar. | Gradle ausente; `main` con asserts ofrece reporting débil; Spring/PostgreSQL no aportan al dominio. |
| Arquitectura | Objeto de aplicación concreto sobre modelo y simulador concretos, todos en un módulo. | Interfaces, factories, registries, puertos y configuración futura. |
| Aislamiento | Catálogos y contribuciones particionados por `TenantId`; se obtiene primero la partición activa y solo ella entra en selección, snapshot y consolidación. | Consulta global con filtrado final. |
| Determinismo | Copias inmutables y orden total explícito por categorías normativas e identificadores opacos. | Orden de inserción, score o desempate implícito. |

## Arquitectura y flujo

`SyntheticScenario` → `SyntheticOnlyGuard` → autorización/partición tenant → resolución de scope o aclaración/denegación → `RetrievalSnapshot` inmutable → `InMemorySimulation` → consolidación validada → `MinimizedTrace`.

La barrera acepta únicamente `InputProvenance.SYNTHETIC` y `SimulationKind.IN_MEMORY_SYNTHETIC`; rechaza cualquier otro valor antes de inspeccionar fixtures. No existe punto de extensión para adaptadores reales.

## Modelo e invariantes

- IDs (`TenantId`, `SourceId`, `ScopeId`, `CandidateId`, `SnapshotId`) son opacos; scopes y snapshots contienen colecciones defensivas ordenadas.
- `Coverage` (`COMPLETE`, `PARTIAL`), `Decision` (`AMBIGUOUS`, `INSUFFICIENT`, `STALE`, `NOT_LOCATED_IN_SCOPE`) e `Impediment` (`DENIED`, `UNAVAILABLE`) son tipos separados. `UNEQUIVOCAL` no pertenece al dominio.
- `RetrievalOutcome` distingue denegación, resultado con candidatos y ausencia en ámbito. La variante de denegación no admite cobertura, decisión ni candidatos. La variante `NOT_LOCATED_IN_SCOPE` fija `COMPLETE`, cero candidatos y ningún impedimento. Los constructores rechazan el resto de combinaciones incompatibles.
- El snapshot copia fuentes, gateways, obligatoriedad y configuración tras resolver scope; mutaciones posteriores del fixture no lo alteran.
- `MinimizedTrace` se construye por lista permitida de categorías e IDs opacos; carece de campo de texto libre, contenido, rutas, consultas, secretos, scores o causas sensibles.

## Estructura mínima propuesta

| Ruta | Finalidad |
|---|---|
| `synthetic-retrieval/pom.xml` | Módulo único y runner reproducible. |
| `synthetic-retrieval/src/main/java/lcia/syntheticretrieval/SyntheticRetrieval.java` | Barrera y flujo. |
| `.../RetrievalModel.java` | Tipos, snapshots e invariantes. |
| `.../InMemorySimulation.java` | Particiones y contribuciones sintéticas. |
| `synthetic-retrieval/src/test/java/lcia/syntheticretrieval/SyntheticRetrievalScenarioTest.java` | Fixtures internos y escenarios. |

Fixtures y snapshots se expresan mediante builders/records Java internos al test o ejecución técnica; no habrá JSON, esquema versionado ni API pública.

## Estrategia de verificación y trazabilidad

Un único conjunto JUnit parametrizado ejecutará escenarios de repetición y permutación, además de comparaciones “entidad cruzada presente/ausente” con salidas y trazas idénticas.

| Requisito | Componente / prueba |
|---|---|
| Autoridad, elegibilidad, mínimo, aclaración, aislamiento | flujo y particiones / contexto inválido, grant opcional, scopes equivalentes y cruce tenant |
| Barrera, snapshot, cobertura, obligatoriedad, decisiones, orden | guard/modelo/simulador / entrada real, mutación posterior, ausencias, permutaciones y combinaciones inválidas |
| Traza categórica, minimización, aislamiento e integridad | `MinimizedTrace` / carga sensible, denegación mínima, cruce tenant y estado incompatible |

`strict_tdd` permanece desactivado durante esta fase; el cambio incorpora el runner que permitirá exigirlo después.

## Amenazas, reversión y diferidos

Matriz de amenazas: N/A; no hay routing, shell, subprocess, VCS, clasificación ejecutable ni integración de procesos.

No hay migración: retirar `synthetic-retrieval/` revierte completamente el cambio. Riesgos: simuladores verdes no prueban seguridad operativa; dependencia inicial del repositorio Maven; crecimiento de un archivo de modelo. Se difieren producción, rendimiento, concurrencia, persistencia, contratos remotos/locales, evidencia por tipo documental y cualquier habilitación de `UNEQUIVOCAL`.
