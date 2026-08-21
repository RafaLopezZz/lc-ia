# Especificación de `minimized-retrieval-trace`

## Propósito

Emitir una traza técnica estructurada, categórica, determinista y minimizada para cada resolución y resultado sintéticos.

## Requisitos

### Requirement: Traza estructurada y categórica

Cada operación **DEBE (MUST)** producir una traza estructurada que represente mediante categorías el resultado de autorización y ámbito, identidad del snapshot, cobertura, decisión, impedimento y procedencia autorizada aplicable. La traza **NO DEBE (MUST NOT)** contener razonamiento libre.

#### Scenario: Resultado multidimensional

- GIVEN una recuperación sintética completada
- WHEN se emite su traza
- THEN cobertura, decisión e impedimento aparecen como dimensiones separadas
- AND las categorías coinciden con el resultado validado

#### Scenario: Aclaración de ámbito

- GIVEN ámbitos autorizados equivalentes
- WHEN se solicita aclaración
- THEN la traza registra la categoría de aclaración y descriptores seguros
- AND no registra texto explicativo libre ni opciones no autorizadas

### Requirement: Minimización de información

La traza **NO DEBE (MUST NOT)** incluir contenido documental, texto completo de consulta, rutas, nombres sensibles, credenciales, secretos, tokens, bytes, extractos, razonamiento, scores no normativos ni afirmaciones factuales. Identificadores y referencias **DEBEN (MUST)** ser sintéticos, opacos y limitados a correlación técnica.

#### Scenario: Carga sensible adversarial

- GIVEN fixtures con campos que simulan contenido, ruta y secreto
- WHEN se genera la traza
- THEN esos campos no aparecen ni directa ni indirectamente
- AND solo permanecen categorías e identificadores opacos permitidos

#### Scenario: Prohibiciones normativas

- GIVEN una salida que intenta registrar `UNEQUIVOCAL` o una afirmación factual
- WHEN se valida la traza
- THEN la traza se rechaza como inválida

### Requirement: Aislamiento observable sin filtración

La traza **DEBE (MUST)** construirse solo con entidades autorizadas del tenant activo. **NO DEBE (MUST NOT)** revelar tenants, ámbitos, fuentes o candidatos no autorizados mediante identificadores, conteos, listas, descriptores, categorías de motivo o diferencias deliberadas de detalle.

#### Scenario: Contribución de otro tenant

- GIVEN una contribución sintética con tenant distinto al snapshot
- WHEN se produce la traza
- THEN no se registra la entidad cruzada ni una categoría que confirme su existencia
- AND conteos y motivos coinciden con un caso donde esa entidad no existe

#### Scenario: Denegación minimizada

- GIVEN una resolución `DENIED`
- WHEN se emite la traza
- THEN solo consta una categoría segura de denegación y correlación sintética
- AND no constan cobertura, candidatos ni causa que distinga recursos ocultos

### Requirement: Determinismo e integridad semántica

Para la misma entrada sintética y snapshot, la traza **DEBE (MUST)** ser idéntica en campos normativos y orden. **DEBE (MUST)** rechazar combinaciones incompatibles entre cobertura, decisión e impedimento en vez de normalizarlas o explicarlas narrativamente.

#### Scenario: Repetición determinista

- GIVEN la misma entrada y snapshot con contribuciones recibidas en distinto orden
- WHEN se genera la traza
- THEN las categorías, candidatos referenciados y orden son idénticos

#### Scenario: Estado semántico inválido

- GIVEN `NOT_LOCATED_IN_SCOPE` junto con `PARTIAL`, candidatos o `UNAVAILABLE`
- WHEN se intenta registrar el resultado
- THEN la traza se rechaza
- AND no sustituye la inconsistencia por razonamiento libre
