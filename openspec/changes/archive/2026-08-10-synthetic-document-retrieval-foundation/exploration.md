# Exploración: base sintética de recuperación documental

## Exploration: `synthetic-document-retrieval-foundation`

### Current State

LC-IA se encuentra en `READY_FOR_SYNTHETIC_DEVELOPMENT`: existen diseños conceptuales coherentes, pero no hay implementación, manifiesto de build, CI ni test runner. El stack objetivo histórico menciona Java, Spring Boot y PostgreSQL, aunque el repositorio no lo ha materializado y los documentos especializados prohíben convertir esa referencia en una elección técnica automática.

La documentación ya confirma las invariantes que esta primera vertical puede validar sin datos reales:

- actor interno, tenant activo, membership vigente y `source grants` directos forman el máximo autorizado;
- pertenecer a un tenant o tener rol administrativo no concede acceso documental;
- existen `source scope` automáticos y `collection scope` administrativos sin jerarquía, herencia ni reglas dinámicas;
- una colección exige grants vigentes para todas sus fuentes, incluidas las opcionales;
- el ámbito se resuelve determinísticamente antes de buscar y su cobertura se congela antes del fan-out;
- `COMPLETE` y `PARTIAL` describen cobertura agregada, mientras `AMBIGUOUS`, `INSUFFICIENT`, `STALE`, `DENIED` y `UNAVAILABLE` describen decisiones o impedimentos en fases distintas;
- una ausencia obligatoria siempre escala; una ausencia opcional mantiene `PARTIAL` y nunca demuestra inexistencia;
- mientras no se defina evidencia suficiente por tipo documental, `UNEQUIVOCAL` y las afirmaciones factuales permanecen bloqueados para datos reales.

No existe todavía una especificación OpenSpec de dominio ni un cambio activo con este nombre.

### Affected Areas

- `openspec/changes/synthetic-document-retrieval-foundation/` — alojará propuesta, especificaciones, diseño, tareas y verificación de la primera vertical sintética.
- `openspec/config.yaml` — fija modo híbrido, `strict_tdd=false`, estrategia `auto-chain`, presupuesto de 800 líneas y ausencia actual de runner.
- `docs/LC-IA-piloto-recuperacion-documental-v0.1.md` — define objetivo funcional, resultados observables, aislamiento y autorización exclusiva para desarrollo sintético.
- `docs/LC-IA-MVP-identidad-tenants-gateways-v0.1.md` — define actor, tenant, memberships, roles y `source grants`, además de impedir acceso documental implícito.
- `docs/LC-IA-MVP-ambito-candidatos-evidencia-v0.1.md` — fuente principal para resolución de scope, catálogo mínimo, abstención y precedencia de resultados.
- `docs/LC-IA-MVP-contrato-remoto-gateways-v0.1.md` — fuente principal para cobertura congelada, fan-out, obligatoriedad y semántica `COMPLETE`/`PARTIAL`.
- `docs/LC-IA-MVP-arquitectura-remota-local-v0.1.md` — mantiene separados los límites remoto/local y excluye transferencia documental de esta base.
- `docs/LC-IA-MVP-fuentes-indexacion-recuperacion-v0.1.md` — aporta vigencia, referencias opacas y estados de fuente, pero su pipeline real de filesystem, OCR e índice queda fuera.

### Approaches

1. **Núcleo de dominio ejecutable con fixtures y simuladores en memoria** — materializar una única unidad ejecutable que reciba fixtures sintéticos, resuelva scope, congele cobertura, simule contribuciones y emita resultados deterministas.
   - Pros: valida las decisiones de mayor riesgo sin red, Auth0, filesystem, OCR, índice, LLM ni base de datos; mantiene los límites explícitos; produce escenarios reproducibles; es compatible con el objetivo Java documentado sin exigir Spring Boot.
   - Cons: exige elegir en diseño versión de Java, herramienta de build y runner; los adaptadores simulados no validan integración, concurrencia real, criptografía ni seguridad operativa.
   - Effort: Medium

2. **Ejecutable mínimo sin herramienta de build ni test runner** — usar solo el runtime y comprobaciones integradas en un punto de entrada.
   - Pros: menor número inicial de archivos y dependencias; hace visible rápidamente el modelo.
   - Cons: verificación y reporting débiles, ejecución menos portable y transición posterior innecesaria; puede convertir una demo en evidencia informal difícil de revisar.
   - Effort: Low

3. **Esqueleto Spring Boot con persistencia PostgreSQL** — comenzar directamente con el stack objetivo histórico y persistir fixtures o decisiones.
   - Pros: se aproxima a una arquitectura futura mencionada en documentación previa.
   - Cons: añade framework, persistencia, configuración y fronteras que esta vertical no necesita; no valida red ni seguridad real; aumenta el riesgo de falsa sensación de avance y contradice el mandato de no materializar stack sin evidencia.
   - Effort: High

4. **Solo matrices documentales de escenarios** — formalizar casos sin crear base ejecutable.
   - Pros: coste mínimo y ninguna elección de stack.
   - Cons: no satisface el propósito de obtener una primera base ejecutable ni prueba determinismo o aislamiento mediante una comprobación reproducible.
   - Effort: Low

### Recommendation

Proponer el enfoque 1 como una sola vertical de dominio, sin Spring Boot, PostgreSQL ni adaptadores productivos. La propuesta debe fijar comportamiento y límites; el diseño debe seleccionar el mínimo toolchain ejecutable tras comprobar el entorno, no por intuición.

#### Corte vertical mínimo propuesto

1. Cargar un catálogo sintético fijo con al menos dos tenants, actores, memberships, grants, fuentes, gateways y una colección reutilizando alguna fuente.
2. Validar actor, tenant activo, membership y grants antes de considerar scopes.
3. Construir scopes elegibles y resolver determinísticamente un `source scope` o `collection scope`; producir aclaración ante equivalencia y denegación ante ausencia autorizada.
4. Congelar una instantánea inmutable de fuentes, gateways y obligatoriedad para la operación.
5. Simular fan-out mediante contribuciones predefinidas, sin red, temporizadores ni concurrencia real.
6. Consolidar candidatos sintéticos conservando tenant, scope, gateway, source, referencia opaca ficticia, versión y categorías de evidencia.
7. Resolver de forma conservadora `AMBIGUOUS`, `INSUFFICIENT`, `STALE`, `DENIED` y `UNAVAILABLE`, manteniendo cobertura `COMPLETE` o `PARTIAL` como dimensión separada.
8. Demostrar ausencia obligatoria y opcional, abstención y aislamiento con escenarios ejecutables deterministas.

#### Escenarios mínimos de aceptación que debe formalizar la especificación

- un actor con grant único resuelve el `source scope` correspondiente;
- una colección se rechaza si falta cualquier grant, aunque la fuente sea opcional;
- dos scopes equivalentes producen aclaración y no selección por orden o score;
- la misma entrada y configuración producen la misma resolución;
- cambiar el fixture después de congelar cobertura no reinterpreta la operación iniciada;
- todas las contribuciones completas producen `COMPLETE`;
- cualquier contribución ausente produce `PARTIAL`;
- una ausencia obligatoria escala aunque existan candidatos;
- una ausencia opcional conserva candidatos y advertencia, sin afirmar inexistencia;
- candidatos plausibles múltiples producen `AMBIGUOUS`;
- evidencia sintética incompleta produce `INSUFFICIENT`;
- versión o referencia discrepante produce `STALE`;
- contexto o grant inválido produce `DENIED` sin filtrar datos de otro tenant;
- gateway o fuente sintética no consultable produce `UNAVAILABLE` y nunca cero resultados concluyente;
- ningún candidato, scope, conteo o motivo revela entidades de otro tenant.

#### Modelo semántico a preservar

No conviene crear un único enum plano. Los documentos describen dimensiones y fases distintas:

| Dimensión | Valores de esta vertical | Regla |
| --- | --- | --- |
| Cobertura agregada | `COMPLETE`, `PARTIAL` | Se deriva de contribuciones contra la cobertura congelada. |
| Decisión de candidato | `AMBIGUOUS`, `INSUFFICIENT`, `STALE` | Se evalúa solo sobre candidatos autorizados y evidencia sintética. |
| Impedimento | `DENIED`, `UNAVAILABLE` | Puede detener el flujo antes o durante la búsqueda. |
| Ausencia de contribución | obligatoria, opcional | Siempre causa `PARTIAL`; la obligatoria escala. |

La representación de “ningún candidato con cobertura `COMPLETE`” necesita decisión explícita en propuesta/especificación. No debe reutilizarse `INSUFFICIENT`, porque la documentación distingue ausencia en el ámbito consultado de evidencia insuficiente sobre candidatos. Puede requerir un resultado funcional separado, sin habilitar afirmaciones universales de inexistencia.

#### Decisiones para propuesta

- confirmar que el cambio termina en resultados sintéticos y no incluye obtención ni bytes;
- adoptar el modelo multidimensional anterior y resolver el nombre normativo del resultado “ninguno”;
- fijar el conjunto mínimo de fixtures y escenarios, incluidos dos tenants y fuentes solapadas entre colecciones;
- declarar explícitamente que `UNEQUIVOCAL` y factual claims no son criterios de éxito de esta vertical;
- establecer que los simuladores son sustitutos controlables, no implementaciones de Auth0, gateway, red, fuente o índice.

#### Decisiones para diseño

- verificar toolchain disponible y elegir versión de Java, build y runner mínimos;
- decidir la estructura mínima de una sola unidad de dominio y sus puntos de entrada, evitando interfaces de una implementación salvo donde el simulador sea una frontera necesaria;
- definir tipos inmutables, precedencia de resultados y combinaciones válidas entre cobertura, decisión e impedimento;
- fijar el formato de fixtures y snapshots sintéticos sin introducir base de datos ni serialización pública prematura;
- definir comparación determinista, desempates estables y auditoría categórica observable;
- diseñar una barrera visible que impida cargar datos o adaptadores reales por accidente.

### Risks

- **Falsa seguridad:** escenarios verdes no prueban autenticación, autorización distribuida, aislamiento de almacenamiento, criptografía, anti-replay, revocación offline ni resistencia a canales laterales.
- **Simulador demasiado obediente:** si las contribuciones solo representan casos felices, la policy parecerá correcta sin afrontar respuestas contradictorias, tardías, duplicadas o de otro tenant.
- **Enum plano:** mezclar cobertura, decisión e impedimento puede permitir combinaciones inválidas y ocultar que `PARTIAL` coexiste con candidatos.
- **Ausencia mal modelada:** convertir cero candidatos en `INSUFFICIENT` o en inexistencia universal contradice la semántica documental.
- **Fixture convertido en política:** valores sintéticos de evidencia, ranking o obligatoriedad no deben convertirse en umbrales productivos.
- **Aislamiento superficial:** filtrar al final no basta; tenant debe formar parte de toda clave, selección y consolidación sintética.
- **Deriva hacia infraestructura:** añadir Spring Boot, PostgreSQL, Auth0, red o filesystem antes de validar el núcleo aumenta coste sin cerrar los riesgos conceptuales.
- **Confusión de `UNEQUIVOCAL`:** una rama sintética positiva podría interpretarse como habilitación sobre datos reales; debe quedar ausente o explícitamente bloqueada.
- **Sobrecarga de alcance:** idempotencia completa, leases, long-polling, descarga y extracción pertenecen a cambios posteriores; aquí solo se simulan contribuciones necesarias para cobertura.

### Ready for Proposal

Yes. El cambio puede pasar a propuesta si conserva una sola vertical sintética de dominio, mantiene `COMPLETE`/`PARTIAL` separados de los estados de decisión, resuelve explícitamente el resultado “ninguno” y deja toolchain, tipos y contratos concretos para diseño. No requiere aclaración humana previa para delimitar el alcance; sí exige que propuesta y especificación eviten presentar la simulación como evidencia de seguridad o readiness operativo.

## Referencias documentales

- `docs/LC-IA-piloto-recuperacion-documental-v0.1.md`, secciones 10, 12, 14 y 17.
- `docs/LC-IA-MVP-identidad-tenants-gateways-v0.1.md`, secciones 3, 5, 6, 9 y 10.
- `docs/LC-IA-MVP-ambito-candidatos-evidencia-v0.1.md`, secciones 4, 7-13, 17-20.
- `docs/LC-IA-MVP-contrato-remoto-gateways-v0.1.md`, secciones 4, 10, 11, 13 y 19.
- `docs/LC-IA-MVP-arquitectura-remota-local-v0.1.md`, secciones 6, 8, 10 y 15.
- `docs/LC-IA-MVP-fuentes-indexacion-recuperacion-v0.1.md`, secciones 7, 9, 15-17.
- `openspec/config.yaml`.
