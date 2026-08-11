# LC-IA — Guía de backlog e importación a Jira

## 1. Modelo de trabajo recomendado

Este backlog está pensado para una sola persona desarrollando LC-IA de manera profesional, con TDD, DDD, arquitectura hexagonal y principios SOLID.

La jerarquía se mantiene deliberadamente pequeña:

- **Epic** = hito técnico/funcional con un gate de salida verificable.
- **Story** = incremento observable de comportamiento.
- **Task** = trabajo técnico necesario para habilitar o proteger comportamiento.

No se usan `Initiative` ni `Sub-task` en esta primera versión. El objetivo es que Jira ayude a ejecutar el proyecto y no se convierta en otra capa de burocracia.

## 2. Hitos

| Epic | Objetivo |
|---|---|
| M0 | Base de ingeniería reproducible |
| M1 | Walking skeleton remoto ↔ gateway |
| M2 | Fuente local e índice mínimo persistente |
| M3 | Autorización de dominio y resolución de ámbito |
| M4 | Autenticación real con Auth0 |
| M5 | Protocolo robusto, fan-out y cobertura |
| M6 | Extracción e indexación de contenido local |
| M7 | Evaluación reproducible de recuperación |
| M8 | Obtención explícita y efímera de documentos |
| M9 | POST-MVP — Capacidades LLM controladas |

`M9` está incluido para conservar la dirección futura, pero **no forma parte del gate de salida del MVP de recuperación documental**.

## 3. Flujo de Jira recomendado

Para un desarrollador único:

```text
BACKLOG
  ↓
READY
  ↓
IN PROGRESS
  ↓
IN REVIEW
  ↓
DONE
```

Usa **WIP = 1** en `IN PROGRESS`.

`BLOCKED` no necesita ser un estado: usa el flag de Jira o una etiqueta mientras exista una dependencia externa o una decisión de seguridad sin cerrar.

### Significado

- **BACKLOG**: trabajo válido, pero todavía no preparado para empezar.
- **READY**: cumple Definition of Ready y puede tomarse sin volver a analizar el proyecto.
- **IN PROGRESS**: existe trabajo activo. Solo un ticket a la vez.
- **IN REVIEW**: implementación terminada; se revisan diff, tests, arquitectura y documentación antes de cerrar.
- **DONE**: cumple íntegramente Definition of Done.

## 4. Definition of Ready

Un ticket solo pasa a `READY` cuando:

1. Tiene un objetivo concreto.
2. Tiene criterios de aceptación observables.
3. Se conocen sus dependencias.
4. Puede identificarse qué prueba debe escribirse primero.
5. No contiene una decisión de seguridad crítica pendiente.
6. El alcance cabe en una PR coherente.
7. No exige diseñar capacidades futuras para completarlo.

Si una decisión es técnica y reversible, el ticket no debe bloquearse indefinidamente: se encapsula detrás de un puerto y se continúa.

## 5. Definition of Done

Todos los tickets de implementación deben cumplir:

- [ ] Se escribió o caracterizó primero una prueba que demuestra el comportamiento esperado.
- [ ] Ciclo TDD completado: RED → GREEN → REFACTOR.
- [ ] Tests unitarios relevantes verdes.
- [ ] Tests de integración/contrato/E2E añadidos cuando el límite lo requiere.
- [ ] Tests ArchUnit siguen verdes.
- [ ] El dominio no depende de Spring, JPA, HTTP ni SDK de infraestructura.
- [ ] No se han introducido `Map<String,Object>` en contratos públicos.
- [ ] No se ha mezclado lógica de autorización con parsing/transporte.
- [ ] `mvn clean test`/`mvn verify` correspondiente pasa desde CLI.
- [ ] `git diff --check` no informa errores.
- [ ] No hay secretos, rutas locales o contenido sensible en logs/respuestas.
- [ ] Documentación/SDD se actualiza únicamente si cambió un contrato o decisión.
- [ ] PR/diff revisado como si lo revisara otra persona.
- [ ] El ticket tiene evidencia suficiente para demostrar sus criterios de aceptación.

## 6. Estrategia TDD por capa

### Dominio

Pruebas unitarias puras.

Ejemplos:

- invariantes;
- máquinas de estado;
- policies;
- value objects;
- autorización;
- resolución de ámbito;
- CandidateResolutionPolicy.

### Aplicación

Tests de casos de uso con fakes.

Ejemplos:

- orquestación;
- idempotencia;
- fan-out;
- consolidación;
- reautorización.

### Puertos y adaptadores

Tests de contrato e integración.

Ejemplos:

- almacenamiento local;
- long-poll;
- Auth0;
- parsers;
- OCR;
- buffer temporal.

### E2E

Pocos y de alto valor.

Cada Epic tiene al menos un gate E2E o una evidencia equivalente.

## 7. DDD y SOLID aplicados al backlog

### DDD

No modelar el sistema como tablas o controladores primero.

Los tickets de dominio deben comenzar por lenguaje e invariantes:

- Actor;
- Tenant;
- Membership;
- SourceGrant;
- Source;
- Scope;
- WorkItem;
- Candidate;
- Coverage;
- TransferRequest.

Los adaptadores se construyen después de que exista un contrato de aplicación o puerto.

### SOLID

- **SRP**: scope resolution, ranking, candidate resolution, authorization y transport son responsabilidades distintas.
- **OCP**: conectores, extractores y futuros modelos se amplían detrás de puertos.
- **LSP**: adaptadores reales y fakes deben respetar los mismos contratos.
- **ISP**: puertos pequeños orientados a capacidades; evitar interfaces “god”.
- **DIP**: dominio/aplicación dependen de abstracciones, no de Spring/Auth0/SQLite/OCR.

SOLID no se evalúa por número de interfaces. Una abstracción solo se introduce cuando protege una frontera o una variación real.

## 8. Git/PR recomendado

Rama:

```text
feat/LCIA-123-short-description
fix/LCIA-123-short-description
chore/LCIA-123-short-description
```

Commits:

```text
feat(scope): resolve minimum authorized document scope
test(gateway): cover required gateway absence
refactor(protocol): isolate work item expiry policy
```

PR:

- un ticket principal por PR;
- enlazar la clave Jira;
- explicar qué comportamiento cambia;
- listar pruebas ejecutadas;
- señalar decisiones/ADR solo cuando sea necesario;
- no mezclar refactor grande con nueva funcionalidad.

## 9. Estimación

No se han incluido Story Points en el CSV deliberadamente.

Primero calibra tu propio ritmo durante M0 y M1. Después puedes estimar usando Fibonacci (`1, 2, 3, 5, 8`) o tamaños (`S/M/L`).

Una Story/Task que estimarías > 8 puntos debería reconsiderarse o dividirse por comportamiento.

No uses la estimación para comprometer fechas mientras el proyecto siga validando riesgos arquitectónicos.

## 10. Importación a Jira Cloud

Archivo:

`LC-IA-jira-backlog-v1.csv`

El CSV usa:

- `Work type`
- `Summary`
- `Work item ID`
- `Parent`
- `Description`
- `Priority`
- `Labels`

Los `Work item ID` son identificadores temporales del CSV. Los hijos referencian el ID numérico de su Epic en `Parent`.

Los Epics aparecen antes que sus hijos.

### Importación recomendada

Para conservar la jerarquía Epic → Story/Task, usa la importación administrativa de CSV desde **External System Import**.

En Jira Cloud, la creación masiva normal por CSV no conserva jerarquías multinivel; la importación administrativa permite mapear `Work item ID` + `Parent`.

Pasos:

1. Crea o elige un proyecto Jira **company-managed**.
2. Comprueba que existen los tipos `Epic`, `Story` y `Task`.
3. Comprueba que las prioridades `Highest`, `High`, `Medium` y `Low` existen. Si no, deja `Priority` sin mapear o adapta los valores del CSV.
4. Abre Jira Administration → System → External System Import → CSV.
5. Selecciona el proyecto de destino.
6. Sube `LC-IA-jira-backlog-v1.csv`.
7. Mapea:
   - `Work type` → Work type / Issue Type
   - `Summary` → Summary
   - `Work item ID` → Work item ID
   - `Parent` → Parent
   - `Description` → Description
   - `Priority` → Priority
   - `Labels` → Labels
8. Revisa la previsualización antes de ejecutar la importación.
9. Abre un Epic y comprueba que sus Story/Task aparecen como hijos.
10. Comprueba M0 y M1 antes de importar cambios adicionales.

Si `Parent` no aparece en el mapeo, revisa la configuración del proyecto/Jira antes de importar; no elimines la jerarquía del CSV como workaround.

## 11. Milestones vs Releases

En esta fase:

- **Epic = milestone de ingeniería.**

No necesitas crear una jerarquía Initiative → Epic.

Cuando M0-M8 estén estabilizados y quieras planificar releases desplegables, puedes añadir Jira `Versions/Releases` por encima del backlog actual, por ejemplo:

- `0.1-synthetic-foundation`
- `0.2-remote-gateway-search`
- `0.3-local-index`
- `0.4-authenticated-search`
- `0.5-retrieval-pilot`

No los he incluido en el CSV porque las versiones y su configuración dependen del proyecto Jira de destino y pueden provocar fallos de importación si no existen previamente.

## 12. Orden de ejecución

No tomes los 77 work items como 77 cosas que debas gestionar simultáneamente.

El orden operativo inicial es:

1. M0.1
2. M0.2
3. M0.3
4. M0.4
5. M0.5
6. M1.1
7. continuar M1 según dependencias

Regla:

> No comenzar un nuevo Epic porque “apetece avanzar” si el gate del Epic actual protege una dependencia real del siguiente.

Hay una excepción: un ticket de otro Epic puede adelantarse si es independiente, reduce riesgo inmediato y no abre trabajo paralelo permanente.

## 13. Política para evitar parálisis por análisis

Antes de crear documentación o un ticket nuevo, clasifica la duda:

### Seguridad / pérdida de datos

Debe cerrarse antes de implementar.

### Técnica reversible

Elegir solución simple detrás de una abstracción y avanzar.

### Solo resoluble con datos

Crear fixture/dataset/test/spike medible. No prolongar el análisis documental.

## 14. Cuándo crear ADR

Crear ADR únicamente si una decisión:

- afecta varios módulos;
- es costosa de revertir;
- cambia un límite de confianza;
- modifica persistencia/protocolo/autoridad;
- condiciona despliegue u operación.

Ejemplos razonables:

- JDK base;
- almacenamiento del índice local;
- estrategia de identidad técnica del gateway;
- almacenamiento temporal de transferencias;
- motor OCR;
- política de evidencia suficiente para UNEQUIVOCAL.

No crear ADR para nombres de métodos, DTOs o decisiones locales reversibles.
