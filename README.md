# LC-IA: núcleo sintético de recuperación documental

Este repositorio contiene un núcleo técnico, reproducible y exclusivamente sintético para validar recuperación documental con autorización, ámbito, cobertura y resultados conservadores. No es una aplicación ni se conecta a servicios o datos externos.

## Inicio rápido

### Requisitos

- JDK 25
- Maven 3.9 o posterior

Desde la raíz del repositorio, compruebe las versiones disponibles:

```bash
java --version
mvn --version
```

Ejecute la batería completa:

```bash
mvn -f synthetic-retrieval/pom.xml test
```

El resultado esperado es `Tests run: 20`, con `Failures: 0`, `Errors: 0` y `Skipped: 0`. Con JDK 25 pueden aparecer advertencias de Jansi o `Unsafe`; no bloquean la ejecución si Maven termina con `BUILD SUCCESS`.

## Qué existe hoy

El módulo `synthetic-retrieval` implementa:

- Barrera de entrada: solo fixtures sintéticos y simulación en memoria.
- Aislamiento por tenant y resolución de ámbitos basada en grants activos.
- Selección determinista del ámbito mínimo o solicitud de aclaración ante alternativas equivalentes.
- Snapshots de recuperación, cobertura `COMPLETE`/`PARTIAL` y consolidación de contribuciones simuladas.
- Resultados conservadores: `AMBIGUOUS`, `INSUFFICIENT`, `STALE` y `NOT_LOCATED_IN_SCOPE`.
- Orden estable de fuentes y candidatos, y traza categórica minimizada.

La batería comprueba la barrera sintética, aislamiento tenant, grants y ámbitos, aclaración, cobertura, resultados conservadores, snapshots, orden determinista y traza minimizada.

## Límites explícitos

No existe UI, API, Auth0, gateways o red, filesystem, OCR, documentos reales, índices, parsers, LLM ni afirmaciones factuales. `UNEQUIVOCAL` no forma parte del dominio de decisión.

No use ni conecte datos reales a este módulo. Sus entradas y contribuciones están diseñadas para fixtures sintéticos en memoria.

## Estructura

```text
synthetic-retrieval/
  pom.xml                                      Configuración Maven y JUnit
  src/main/java/lcia/syntheticretrieval/
    SyntheticRetrieval.java                    Barrera de procedencia sintética
    RetrievalModel.java                         Modelo de ámbitos, snapshots, resultados y traza
    InMemorySimulation.java                     Autorización y consolidación simuladas
  src/test/java/lcia/syntheticretrieval/
    SyntheticRetrievalScenarioTest.java         Escenarios verificables del núcleo
```

## Diagnóstico mínimo

| Situación | Acción |
|---|---|
| `java` no está disponible o no informa versión 25 | Instale o seleccione JDK 25 y vuelva a abrir la terminal. |
| `mvn` no está disponible o es anterior a 3.9 | Instale Maven 3.9 o posterior y asegure que esté en `PATH`. |
| Un test falla | Ejecute de nuevo el comando completo, revise el nombre del test fallido y compare el resultado con los escenarios de `SyntheticRetrievalScenarioTest`. No sustituya los fixtures sintéticos por datos reales. |

## Siguiente paso

Usar esta suite como base para decidir y especificar la próxima integración, manteniendo la separación entre el núcleo sintético actual y cualquier adaptación de infraestructura futura.
