# Especificación de `authorized-scope-resolution`

## Propósito

Resolver un único ámbito autorizado y reproducible antes de cualquier búsqueda sintética, sin revelar entidades fuera del tenant activo.

## Requisitos

### Requirement: Autoridad previa al ámbito

La resolución **DEBE (MUST)** validar actor, tenant activo, membership vigente y contexto operativo. Intención, pistas, roles y referencias **NO DEBEN (MUST NOT)** conceder acceso ni ampliar el conjunto determinado por `source grants` vigentes.

#### Scenario: Contexto autorizado

- GIVEN un actor sintético con tenant activo y membership vigentes
- WHEN solicita resolver un ámbito cubierto por sus grants
- THEN solo se consideran fuentes de ese tenant concedidas al actor

#### Scenario: Contexto o grant inválido

- GIVEN tenant activo discrepante, membership suspendida o grant revocado
- WHEN se intenta resolver el ámbito
- THEN el resultado es `DENIED` antes de buscar
- AND no revela existencia, conteos ni motivos distinguibles sobre entidades no autorizadas

### Requirement: Elegibilidad de tipos de ámbito

Un `source scope` **DEBE (MUST)** exigir grant vigente para su única fuente. Un `collection scope` **DEBE (MUST)** exigir grants vigentes para todas sus fuentes explícitas, incluidas las opcionales; colección y opcionalidad **NO DEBEN (MUST NOT)** conceder permisos.

#### Scenario: Source scope elegible

- GIVEN una fuente sintética del tenant activo con grant vigente
- WHEN esa fuente satisface la intención dentro del contexto validado
- THEN su `source scope` es elegible

#### Scenario: Collection scope íntegramente autorizado

- GIVEN una colección sintética cuyas fuentes obligatorias y opcionales tienen grants vigentes
- WHEN la colección satisface la intención
- THEN su `collection scope` es elegible sin duplicar fuentes compartidas

#### Scenario: Grant opcional ausente

- GIVEN una colección con una fuente opcional sin grant vigente
- WHEN se evalúa la colección
- THEN el `collection scope` completo no es elegible
- AND la fuente no se omite para construir una vista parcial autorizada

### Requirement: Selección y aclaración deterministas

La resolución **DEBE (MUST)** elegir el menor ámbito autorizado que satisfaga la intención y conserve las fuentes obligatorias aplicables. Con las mismas entradas autoritativas y configuración, **DEBE (MUST)** producir el mismo resultado. Ámbitos equivalentes **DEBEN (MUST)** producir aclaración estructurada, nunca selección por orden, score o modelo.

#### Scenario: Ámbito único mínimo

- GIVEN un ámbito autorizado suficiente y otro más amplio
- WHEN ambos son compatibles con la intención
- THEN se selecciona el menor que conserva las fuentes obligatorias aplicables

#### Scenario: Ámbitos equivalentes

- GIVEN dos ámbitos autorizados igualmente válidos
- WHEN ninguno puede distinguirse mediante contexto autorizado
- THEN se solicita aclaración con descriptores seguros y estructurados
- AND solo se incluyen opciones autorizadas, sin contenido ni rutas

#### Scenario: Repetición estable

- GIVEN idénticas entradas autoritativas y configuración sintética
- WHEN la resolución se repite
- THEN ámbito, aclaración o `DENIED` son idénticos

### Requirement: Aislamiento por construcción

Toda selección, clave lógica, comparación y aclaración **DEBE (MUST)** estar acotada al tenant activo desde el origen. El sistema **NO DEBE (MUST NOT)** consultar globalmente y filtrar al final.

#### Scenario: Cruce adversarial de tenant

- GIVEN pistas que coinciden mejor con una fuente de otro tenant
- WHEN se resuelve el ámbito en el tenant activo
- THEN esa fuente nunca entra en el conjunto evaluado
- AND la salida no cambia en conteos, descriptores ni motivos por su existencia
