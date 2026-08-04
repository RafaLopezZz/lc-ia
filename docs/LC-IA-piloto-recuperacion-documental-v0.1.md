---
title: "LC-IA - Piloto de recuperación documental"
status: "Especificación funcional preliminar v0.1"
project: "LC-IA / Plataforma de Leos"
date: "2026-08-01"
scope: "Recuperación general de documentos mediante lenguaje natural"
---

# LC-IA - Piloto de recuperación documental

Este documento define el alcance funcional mínimo del primer piloto de LC-IA: permitir que una persona autorizada solicite un documento en lenguaje natural y que LeoDocumental lo recupere exclusivamente desde fuentes registradas y autorizadas. Sirve para cerrar decisiones antes del diseño técnico y de una futura formalización mediante requisitos o SDD; no es un artefacto SDD ni prescribe clases, APIs, base de datos o proveedores.

## 1. Lectura rápida

| Tema | Definición actual |
| --- | --- |
| Capacidad piloto | Recuperar documentos de distintos tipos a partir de una solicitud en lenguaje natural. |
| Delegación | El orquestador delega en LeoDocumental cuando identifica una intención de recuperación documental. |
| Primera fuente | Una carpeta local o compartida, registrada y autorizada. |
| Límite clave | La ubicación expresada por el usuario es una pista, nunca una ruta de acceso autoritativa. |
| Resultado | Un documento inequívoco, varios candidatos, ninguno, acceso denegado o fuente no disponible. |
| No incluido | Extracción estructurada, sumas, cálculos y acciones sobre los documentos. |
| Estado | `READY_FOR_SYNTHETIC_DEVELOPMENT`; no preparado para documentos reales, habilitación operativa ni producción. |

## 2. Naturaleza y uso

### 2.1. Propósito

Este artefacto debe permitir que una siguiente conversación de diseño:

- valide que el piloto resuelve un problema real y acotado;
- convierta decisiones confirmadas en requisitos verificables;
- identifique las preguntas que bloquean la implementación;
- prepare un dataset de evaluación antes de elegir una estrategia técnica;
- evite construir una plataforma documental general antes de probar la recuperación.

### 2.2. Fuentes consultadas

- `docs/LC-IA-documento-maestro-contexto-requisitos-casos-uso-hitos-TDD-SDD-v0.5.md`;
- `docs/analisis-requisitos-leo-documental-v0.4.md`;
- `docs/LC-IA-inventario-modelos-clases-servicios-controladores-v0.1.md`, consultado solo para detectar y evitar sobrearquitectura.

### 2.3. Regla de interpretación

Las decisiones confirmadas de este documento prevalecen para el alcance del piloto. Las hipótesis no son requisitos aceptados. Las preguntas pendientes no deben cerrarse mediante suposiciones durante la implementación.

## 3. Problema y resultado esperado

### 3.1. Problema

Una persona puede conocer datos parciales de un documento, como empresa, fecha aproximada, tipo, asunto o posible ubicación, pero no su nombre exacto ni su ruta registrada. La búsqueda manual obliga a recorrer carpetas y nombres de archivo, y puede terminar en un documento incorrecto, inaccesible o inexistente.

### 3.2. Propuesta del piloto

La persona formula la necesidad en lenguaje natural. LC-IA reconoce la intención de recuperar documentos y delega la tarea en LeoDocumental. Este interpreta las pistas, busca solo en fuentes que el sistema conoce y que el actor puede consultar, y devuelve un resultado explícito sin inventar accesos ni ocultar ambigüedades.

Ejemplos de solicitudes dentro del alcance:

- "Recupera el documento escaneado correspondiente al albarán de la empresa X".
- "Busca el contrato de mantenimiento de la empresa X".
- "Encuentra la ficha técnica del equipo instalado en el proyecto Y".
- "Localiza el informe enviado aproximadamente en marzo sobre la incidencia Z".

El ejemplo de albarán ilustra una consulta posible; no especializa el piloto en albaranes.

### 3.3. Resultado de aprendizaje

El piloto debe permitir decidir, con evidencia de evaluación, si la recuperación mediante lenguaje natural encuentra documentos autorizados con calidad y tiempo suficientes para justificar una fase posterior de implementación o ampliación.

## 4. Actores

| Actor | Responsabilidad en el piloto |
| --- | --- |
| Solicitante autorizado | Formula la petición y recibe el resultado dentro de su alcance. |
| Responsable de fuentes | Registra o aprueba las fuentes consultables y su pertenencia al tenant. |
| Evaluador del piloto | Etiqueta consultas, valida el documento objetivo y revisa errores o ambigüedades. |
| Orquestador LC-IA | Reconoce la intención y delega la recuperación en LeoDocumental. |
| LeoDocumental | Resuelve pistas, consulta conectores autorizados y presenta el resultado. |

Los nombres concretos de personas, organizaciones, roles y responsables quedan pendientes.

## 5. Alcance mínimo

### 5.1. Incluido

- peticiones de recuperación en lenguaje natural;
- distintos tipos documentales presentes en las fuentes autorizadas;
- pistas como tipo, entidad relacionada, fecha, asunto, identificadores parciales y ubicación mencionada;
- búsqueda en una o más fuentes registradas que sean accesibles para el tenant y actor;
- primer conector a una carpeta local o compartida;
- identificación y entrega del documento o de candidatos verificables;
- respuesta explícita para éxito, ambigüedad, ausencia, denegación e indisponibilidad;
- registro mínimo de la consulta, fuentes consideradas, resultado, latencia y motivo de no éxito, con contenido minimizado.

### 5.2. Fuera del piloto

- extracción estructurada de campos;
- lectura para sumar costes, importes u otras cantidades;
- cálculos, agregaciones o conciliaciones;
- clasificación automática como resultado de negocio;
- modificación, traslado, renombrado o borrado de documentos;
- escritura en ERP, correo u otros sistemas;
- repositorios arbitrarios aportados por el usuario;
- rastreo general del sistema de archivos;
- conectores adicionales al primero, salvo simulaciones necesarias para validar el contrato común;
- APIs públicas, paneles de administración y esquemas de persistencia definitivos;
- elección anticipada de búsqueda léxica, vectorial, motor OCR, LLM o proveedor concreto;
- una plataforma genérica de agentes, coordinación multi-Leo o memoria conversacional avanzada.

## 6. Decisiones confirmadas

| ID | Decisión |
| --- | --- |
| D-01 | El primer piloto es recuperación general de documentos mediante lenguaje natural. |
| D-02 | El piloto admite distintos tipos documentales y no se limita a albaranes. |
| D-03 | No incluye extracción estructurada, suma de costes ni cálculos. |
| D-04 | El orquestador delega en LeoDocumental cuando la intención es recuperar documentos. |
| D-05 | El acceso a repositorios se expresa mediante un contrato común que permita incorporar conectores sin cambiar el objetivo funcional. |
| D-06 | El primer conector accede a una carpeta local o compartida. |
| D-07 | Una ubicación indicada por el usuario es solo una pista que debe resolverse contra fuentes registradas y autorizadas. |
| D-08 | No se aceptan rutas, credenciales o repositorios arbitrarios como autoridad de acceso. |
| D-09 | El resultado debe distinguir un documento inequívoco, varios candidatos, ninguno, acceso denegado y fuente no disponible. |

## 7. Hipótesis por validar

| ID | Hipótesis | Cómo se observará |
| --- | --- | --- |
| H-01 | Las personas pueden expresar suficientes pistas en lenguaje natural para localizar una parte relevante de sus documentos. | Evaluación con consultas reales y documento objetivo conocido. |
| H-02 | Los nombres, metadatos y contenido disponibles permiten diferenciar documentos sin extracción estructurada de negocio. | Resultados de recuperación y análisis de consultas fallidas. |
| H-03 | Un contrato común de fuente/conector permite comenzar con carpetas sin acoplar el caso de uso a esa tecnología. | La especificación funcional del conector no expone detalles de carpetas al flujo principal. |
| H-04 | La ambigüedad puede mostrarse de forma útil como candidatos, sin elegir arbitrariamente. | Revisión humana de los casos con varios candidatos. |
| H-05 | El corpus contiene suficientes documentos legibles o metadatos útiles para evaluar recuperación. | Inventario del corpus y cobertura del dataset, incluidos documentos escaneados. |

Estas hipótesis no autorizan a fijar tecnología, cifras objetivo o comportamiento no descrito.

## 8. Modelo conceptual mínimo de fuente y conector

### 8.1. Fuente registrada

Una **fuente registrada** representa un repositorio conocido por LC-IA y habilitado para un tenant. Conceptualmente necesita solo:

- identificador estable;
- nombre comprensible para operación;
- tenant propietario o autorizado;
- tipo de conector;
- referencia de configuración gestionada por el sistema, no aportada libremente en cada consulta;
- estado de disponibilidad o habilitación;
- alcance de actores autorizados, si es más restrictivo que el tenant.

### 8.2. Conector

Un **conector** traduce una consulta documental común a las capacidades de una fuente y devuelve candidatos identificables. El contrato conceptual debe cubrir:

- buscar usando texto y pistas permitidas;
- devolver identidad estable, nombre, tipo si se conoce, ubicación presentable, procedencia y señales de relevancia;
- comprobar o conservar el contexto seguro de tenant y actor;
- distinguir resultado vacío, acceso denegado e indisponibilidad técnica;
- recuperar o referenciar el documento seleccionado sin escapar de la fuente registrada.

El contrato no implica una interfaz Java, un servicio por operación, un registro de clases ni un esquema de base de datos en esta fase.

### 8.3. Resolución segura de una pista de ubicación

Si el usuario escribe "en la carpeta de la empresa X" o incluye una ruta:

1. LeoDocumental interpreta el texto como pista de búsqueda.
2. El sistema obtiene las fuentes registradas que el tenant y actor pueden consultar.
3. La pista se compara con nombres, alias o ubicaciones conocidas dentro de esas fuentes.
4. La búsqueda se ejecuta solo en las fuentes autorizadas resultantes.
5. Si la pista apunta fuera de ellas, no se accede a esa ubicación y se devuelve acceso denegado o ausencia de fuente autorizada, según corresponda.

Nunca se abre directamente una ruta suministrada por el usuario ni se descubren repositorios por exploración libre.

## 9. Flujo principal

1. Un actor autenticado formula una petición en lenguaje natural.
2. El orquestador identifica una intención de recuperación documental.
3. El orquestador delega la petición y el contexto seguro en LeoDocumental.
4. LeoDocumental extrae pistas de recuperación sin tratarlas como permisos.
5. El sistema resuelve las fuentes registradas y autorizadas para el tenant y actor.
6. Los conectores disponibles buscan candidatos dentro de esas fuentes.
7. LeoDocumental consolida candidatos sin ocultar procedencia ni ambigüedad.
8. El sistema produce exactamente uno de los resultados funcionales definidos.
9. Se registra evidencia mínima para evaluar calidad, latencia y causa del resultado.

El flujo no requiere extraer campos de negocio ni generar una respuesta narrativa sobre el contenido del documento.

## 10. Resultados funcionales

| Resultado | Condición observable | Respuesta mínima esperada |
| --- | --- | --- |
| Documento inequívoco | Un candidato autorizado satisface de forma suficiente las pistas y no existe ambigüedad material conocida. | Documento o referencia recuperable, procedencia y pistas que justifican la selección. |
| Varios candidatos | Dos o más candidatos autorizados siguen siendo plausibles. | Lista ordenada de candidatos con procedencia y datos suficientes para que el actor elija o aclare. |
| Ninguno | Las fuentes autorizadas disponibles se consultaron sin encontrar candidato suficiente. | Indicar que no se encontró el documento y qué fuentes fueron consideradas, sin afirmar que el documento no existe fuera de ellas. |
| Acceso denegado | El actor solicita o sugiere una fuente o documento que no puede consultar, o intenta aportar una ruta arbitraria. | Denegación explícita sin revelar contenido, existencia sensible ni detalles de una fuente no autorizada. |
| Fuente no disponible | Una fuente autorizada necesaria no puede consultarse por un fallo técnico o estado inactivo. | Indisponibilidad explícita, fuente afectada cuando sea seguro mostrarla y ausencia de falso resultado negativo. |

Los criterios para considerar un documento "inequívoco" y el número máximo de candidatos quedan pendientes de calibración con el dataset real.

## 11. Escenarios funcionales

### E-01. Recuperación inequívoca

**Dado** un actor autorizado y una fuente disponible con el documento objetivo,  
**cuando** solicita "recupera el documento escaneado correspondiente al albarán de la empresa X" y las pistas diferencian un único documento,  
**entonces** recibe ese documento o una referencia utilizable, su procedencia y la justificación mínima de coincidencia.

### E-02. Recuperación de otro tipo documental

**Dado** que la fuente contiene contratos, informes u otros tipos,  
**cuando** el actor pide uno de esos documentos mediante pistas parciales,  
**entonces** se aplica el mismo flujo sin exigir que sea un albarán.

### E-03. Varios candidatos válidos

**Dado** que varios documentos coinciden de forma plausible,  
**cuando** no existe evidencia suficiente para elegir uno,  
**entonces** LeoDocumental devuelve los candidatos y no selecciona arbitrariamente.

### E-04. Documento no encontrado

**Dado** que las fuentes autorizadas están disponibles,  
**cuando** ningún documento satisface suficientemente la solicitud,  
**entonces** el resultado es "ninguno" y no una respuesta inventada.

### E-05. Pista de ubicación autorizada

**Dado** que el actor menciona una carpeta mediante un nombre o ruta aproximada,  
**cuando** esa pista se corresponde con una fuente registrada y autorizada,  
**entonces** la búsqueda se limita a esa fuente o la usa como señal de preferencia según la configuración aceptada.

### E-06. Ruta arbitraria o fuente no autorizada

**Dado** que el actor aporta una ruta o repositorio fuera de sus fuentes autorizadas,  
**cuando** solicita buscar en él,  
**entonces** el sistema no accede, no explora rutas cercanas y devuelve acceso denegado sin filtrar información sensible.

### E-07. Fuente no disponible

**Dado** que una fuente autorizada no responde o está inactiva,  
**cuando** resulta necesaria para atender la consulta,  
**entonces** se informa de indisponibilidad y no se presenta "ninguno" como si la búsqueda hubiera sido completa.

### E-08. Documento escaneado sin contenido recuperable

**Dado** un documento escaneado cuyos metadatos o texto disponible no permiten identificarlo,  
**cuando** participa en una consulta,  
**entonces** el caso queda registrado como limitación de recuperación y no como prueba de inexistencia. OCR local está confirmado para el MVP; su diseño y decisiones pendientes se rigen por [`LC-IA-MVP-fuentes-indexacion-recuperacion-v0.1.md`](LC-IA-MVP-fuentes-indexacion-recuperacion-v0.1.md).

### E-09. Aislamiento entre tenants o actores

**Dado** un documento existente fuera del alcance del actor,  
**cuando** las pistas podrían identificarlo,  
**entonces** no aparece como candidato ni se revela su contenido o ubicación.

## 12. Criterios de aceptación observables

| ID | Criterio |
| --- | --- |
| CA-01 | Una consulta clasificada como recuperación documental es delegada a LeoDocumental. |
| CA-02 | El mismo flujo admite al menos los tipos documentales representados en el dataset aceptado, sin reglas exclusivas para albaranes. |
| CA-03 | Toda búsqueda se limita a fuentes registradas y autorizadas para el tenant y actor. |
| CA-04 | Una ruta o repositorio aportado libremente nunca se usa como autoridad de acceso. |
| CA-05 | Cada ejecución produce uno de los cinco resultados funcionales definidos. |
| CA-06 | Un resultado inequívoco identifica el documento y su fuente de procedencia. |
| CA-07 | Ante ambigüedad material se devuelven candidatos; no se elige uno sin evidencia suficiente. |
| CA-08 | "Ninguno" solo se emite cuando las fuentes autorizadas previstas fueron consultables. |
| CA-09 | Una fuente indisponible se diferencia de una búsqueda sin coincidencias. |
| CA-10 | Un acceso denegado no revela contenido ni ubicaciones no autorizadas. |
| CA-11 | La evaluación puede comparar candidatos devueltos con el documento objetivo del dataset. |
| CA-12 | Se mide latencia y se registra el tipo de resultado sin fijar todavía un SLA. |
| CA-13 | El piloto no ejecuta extracción estructurada, sumas, cálculos ni modificaciones documentales. |

## 13. Evaluación del piloto

### 13.1. Métricas provisionales propuestas

Estas métricas son propuestas hasta disponer de un dataset real. Sus valores objetivo, `k`, percentiles y criterios de éxito quedan pendientes.

| Métrica propuesta | Interpretación |
| --- | --- |
| `success@k` | Proporción de consultas con documento objetivo en los primeros `k` candidatos. Puede ser la métrica primaria si cada consulta tiene un objetivo inequívoco. |
| `precision@k` | Proporción de los primeros `k` candidatos que son relevantes. Es útil cuando puede haber varios documentos válidos. |
| Latencia de recuperación | Tiempo desde la recepción de la solicitud hasta el resultado funcional; se informará mediante percentiles acordados, sin SLA previo. |
| Tasa de ambigüedad | Proporción de consultas que devuelven varios candidatos. Debe analizarse junto con la corrección de esas abstenciones. |
| Tasa de resultados "ninguno" | Proporción de consultas sin candidato, separando objetivos inexistentes de fallos de recuperación. |
| Tasa de denegación correcta | Proporción de casos de acceso denegado en los que no se devuelve información protegida. |
| Tasa de indisponibilidad | Proporción de consultas afectadas por fuentes no disponibles, separada de la calidad de búsqueda. |

No se fijan cifras objetivo antes de conocer corpus, consultas y línea base. Tampoco se asume que una sola métrica represente valor de negocio y seguridad a la vez.

### 13.2. Dataset de evaluación pendiente

El dataset debe versionarse y ser revisado por una persona que conozca el corpus. Cada caso debe contener, como mínimo:

- consulta real o representativa redactada como la formularía el usuario;
- tenant y actor de prueba o alcance autorizado equivalente;
- fuentes que pueden consultarse;
- documento objetivo o indicación explícita de que no existe;
- conjunto de documentos también válidos, cuando la respuesta no sea única;
- pistas relevantes conocidas, sin obligar a que aparezcan literalmente en el archivo;
- resultado esperado: inequívoco, varios candidatos, ninguno, acceso denegado o fuente no disponible;
- observaciones para explicar ambigüedad, OCR o limitaciones del corpus.

El conjunto debe incluir:

- consultas reales de distintos tipos documentales;
- documentos objetivo con nombres y ubicaciones poco evidentes;
- casos ambiguos con varios candidatos plausibles;
- documentos escaneados, con y sin texto OCR disponible;
- consultas sobre documentos inexistentes;
- intentos de acceso denegado entre tenants, actores o fuentes;
- fuentes simuladas como no disponibles;
- variaciones de una misma consulta para observar sensibilidad a la redacción.

El número de casos, su distribución y el tratamiento de datos sensibles quedan pendientes. No se inventa un mínimo hasta revisar el corpus real.

## 14. Seguridad mínima

1. El tenant y el actor proceden de un contexto autenticado; no los elige el texto de la consulta.
2. Solo se consultan fuentes registradas, habilitadas y autorizadas para ese contexto.
3. Los permisos se comprueban antes de buscar y antes de entregar el documento o referencia.
4. Una pista de ubicación nunca amplía el alcance autorizado.
5. Las credenciales y referencias técnicas de una fuente no se exponen al usuario ni forman parte libre de la consulta.
6. Los resultados no revelan documentos, nombres sensibles o ubicaciones de fuentes no autorizadas.
7. Ante duda de autorización, se deniega el acceso en lugar de intentar la búsqueda.
8. Los registros de evaluación y operación minimizan contenido documental y datos personales.

La identidad concreta, los roles, la gestión de credenciales, la retención, el cifrado y los requisitos regulatorios aplicables deben definirse antes de usar datos reales; no se presuponen en este documento.

## 15. Preguntas pendientes

### 15.1. Bloquean la preparación para implementar

| ID | Pregunta |
| --- | --- |
| P-01 | ¿Qué tenant o entorno aislado participará y quiénes serán solicitantes, responsables de fuentes y evaluadores? |
| P-02 | ¿Qué carpeta local o compartida se registrará y quién confirma que su contenido puede usarse? |
| P-03 | ¿Qué formatos y tipos documentales reales contiene el corpus inicial? |
| P-04 | ¿Cómo se identifica y autentica al actor durante el piloto? |
| P-05 | ¿Qué regla de autorización relaciona actores con fuentes y, si aplica, con subconjuntos documentales? |
| P-06 | ¿Cómo se entregará un documento encontrado: apertura, descarga, enlace o referencia? |
| P-07 | ¿Qué pistas y metadatos ofrece realmente la carpeta y qué contenido es legible sin OCR adicional? |
| P-08 | ¿Qué dataset versionado se usará y quién valida cada documento objetivo? |
| P-09 | ¿Cuál será la métrica primaria, qué valor de `k` se evaluará y qué umbrales determinan continuar, pivotar o parar? |
| P-10 | ¿Qué criterio operativo distingue documento inequívoco de varios candidatos? |
| P-11 | ¿Qué canal o interfaz usará el solicitante durante el piloto? |
| P-12 | ¿Qué datos de consulta y resultado pueden conservarse para evaluar sin exceder la finalidad autorizada? |

### 15.2. Pueden cerrarse durante el diseño técnico

- estrategia inicial de recuperación y ranking;
- parámetros y validación de OCR local conforme a [`LC-IA-MVP-fuentes-indexacion-recuperacion-v0.1.md`](LC-IA-MVP-fuentes-indexacion-recuperacion-v0.1.md);
- forma concreta del contrato de conector;
- gestión de cambios, renombrados y documentos duplicados en la carpeta;
- comportamiento cuando varias fuentes autorizadas coinciden con una pista de ubicación;
- límites de candidatos y tiempos de espera;
- detalle de trazabilidad y observabilidad técnica.

No se han definido organizaciones, repositorios concretos, volúmenes, SLA, cifras objetivo, proveedores ni requisitos regulatorios.

## 16. Riesgos del piloto

| Riesgo | Efecto | Tratamiento previo |
| --- | --- | --- |
| Corpus no representativo | Métricas que no predicen utilidad real. | Construir el dataset con consultas y documentos reales autorizados. |
| Documentos escaneados sin texto útil | Falsos negativos atribuidos al buscador. | Etiquetar casos OCR y medirlos por separado antes de decidir tecnología. |
| Ambigüedad no visible | Entrega de un documento incorrecto. | Devolver candidatos cuando no exista evidencia suficiente. |
| Pistas tratadas como permisos | Acceso a rutas o fuentes arbitrarias. | Resolver siempre contra el registro y autorización del sistema. |
| Mezcla entre ausencia e indisponibilidad | Conclusión falsa de que un documento no existe. | Mantener resultados funcionales separados. |
| Sobrearquitectura | Retraso y validación tardía del valor. | Implementar solo el flujo, el primer conector y la evaluación necesarios. |
| Métricas sin verdad de referencia | Resultados imposibles de interpretar. | Exigir documento objetivo y revisión humana del dataset. |

## 17. Gate de preparación para implementar con datos reales

La implementación del piloto con datos o documentos reales y su habilitación operativa puede comenzar solo cuando todos los puntos siguientes tengan una respuesta verificable:

Se autoriza desarrollo ejecutable con datos exclusivamente sintéticos para validar contratos conceptuales, estados, aislamiento, idempotencia, resolución de ámbito y abstención. Esta autorización no habilita identidades, fuentes, metadatos ni documentos reales, ni demuestra que los controles de seguridad estén terminados.

- [ ] tenant o entorno de piloto identificado;
- [ ] actores y responsables identificados;
- [ ] primera carpeta registrada, autorizada y accesible en el entorno de prueba;
- [ ] formatos y tipos documentales iniciales inventariados;
- [ ] forma de autenticación y autorización acordada;
- [ ] forma de entrega del documento acordada;
- [ ] dataset inicial versionado con verdad de referencia y los casos exigidos;
- [ ] métrica primaria, métricas secundarias y método de cálculo aceptados;
- [ ] umbrales provisionales y criterio de continuar, pivotar o parar aceptados;
- [ ] definición observable de documento inequívoco y ambigüedad aceptada;
- [ ] tratamiento de documentos escaneados decidido para la primera evaluación;
- [ ] reglas de minimización y uso de datos reales aprobadas;
- [ ] escenarios y criterios de aceptación de este documento revisados por las personas responsables.

Si falta cualquiera de estos elementos, la siguiente actividad para usar datos reales o habilitar operación debe ser cerrar la decisión o preparar datos, no crear módulos, controladores o integraciones adicionales sobre información real.

## 18. Próximos pasos priorizados

1. Identificar tenant, actores, responsables y primera carpeta autorizada.
2. Inventariar una muestra del corpus real: tipos, formatos, metadatos, duplicados y estado de OCR.
3. Construir y revisar el dataset de evaluación con verdad de referencia y los cinco resultados funcionales.
4. Elegir métrica primaria, valor de `k`, métricas secundarias y criterios provisionales de continuar, pivotar o parar.
5. Cerrar la definición de resultado inequívoco, entrega del documento y tratamiento de ambigüedad.
6. Validar autenticación, autorización y minimización para el entorno de piloto.
7. Solo tras superar el gate, habilitar datos o documentos reales y operación del piloto.

## 19. Criterio de cierre del piloto

El piloto no se considera exitoso por completar una demostración aislada. Su cierre debe producir:

- resultados reproducibles sobre el dataset versionado;
- desglose por tipo de resultado y por limitaciones de OCR o fuente;
- comparación con los umbrales que se acuerden antes de evaluar;
- incidentes de seguridad o denegaciones incorrectas, si los hubiera;
- decisión explícita de continuar, pivotar o parar;
- preguntas y riesgos residuales para una eventual fase SDD.

Hasta que existan dataset y umbrales aceptados, no puede declararse preparado para documentos reales, operación o producción ni exitoso. Su readiness actual es `READY_FOR_SYNTHETIC_DEVELOPMENT`.
