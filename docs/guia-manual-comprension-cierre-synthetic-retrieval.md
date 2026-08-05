# Guía manual para comprender y cerrar `synthetic-document-retrieval-foundation`

> **Objetivo**
>
> Comprender el núcleo sintético de recuperación documental paso a paso, demostrar cada afirmación con evidencia y cerrar el cambio sin depender del flujo bloqueado de Gentle AI.
>
> La guía está escrita para PowerShell en Windows y supone que el repositorio contiene:
>
> - `openspec/changes/synthetic-document-retrieval-foundation/`;
> - un módulo Maven llamado `synthetic-retrieval`;
> - la suite `SyntheticRetrievalScenarioTest`;
> - `verify-report.md`.
>
> Si algún nombre o ruta no coincide, **no lo inventes**: búscalo con los comandos de descubrimiento incluidos.

---

## 1. El mapa mental más sencillo

Imagina que estás construyendo un castillo de LEGO:

- **OpenSpec** es el plano del castillo.
- **El código Java** son las piezas construidas.
- **Los tests** comprueban que las puertas, paredes y torres funcionan como indica el plano.
- **La evidencia** es una foto o un registro que demuestra qué comprobaste.
- **Archivar el cambio** significa declarar: “este castillo ya coincide con este plano”.

Todavía no estamos construyendo una ciudad real. `synthetic-retrieval` es una maqueta segura para aprender y comprobar las reglas antes de conectar usuarios, bases de datos, documentos o gateways reales.

---

# Parte I — Observar sin modificar el repositorio

## Paso 0. Regla de oro: una afirmación necesita una prueba

Antes de empezar, usa esta regla:

> No escribir “funciona” sin poder señalar el comando, el test o el archivo que lo demuestra.

Para cada descubrimiento anota:

```text
Afirmación:
Evidencia:
Archivo o test:
Resultado:
Duda pendiente:
```

Ejemplo:

```text
Afirmación:
La ausencia de un gateway obligatorio escala el resultado.

Evidencia:
Test requiredGatewayAbsenceEscalatesAnOtherwiseAvailableResult.

Resultado:
PASS.

Duda pendiente:
Comprobar en el código qué condición crea la escalación.
```

---

## Paso 1. Crear una carpeta de evidencias fuera del repositorio

### Idea sencilla

Queremos guardar “fotografías” del trabajo sin ensuciar el proyecto.

La carpeta de evidencia estará en `Documentos`, no dentro del repositorio.

### Ejecuta

```powershell
$EvidenceRoot = Join-Path `
    $env:USERPROFILE `
    ("Documents\LC-IA-evidence\synthetic-retrieval-" + (Get-Date -Format "yyyyMMdd-HHmmss"))

New-Item -ItemType Directory -Path $EvidenceRoot -Force | Out-Null

Start-Transcript -Path (Join-Path $EvidenceRoot "00-transcript.txt")
```

### Comprueba

```powershell
$EvidenceRoot
```

### Evidencia esperada

Debe aparecer una ruta parecida a:

```text
C:\Users\Rafa\Documents\LC-IA-evidence\synthetic-retrieval-20260805-101500
```

### Continúa cuando

- la carpeta exista;
- `Start-Transcript` indique que comenzó la transcripción.

> **Transcripción** significa que PowerShell guardará en un archivo los comandos y mensajes de la sesión.

---

## Paso 2. Entrar al repositorio y registrar el punto de partida

### Idea sencilla

Antes de tocar un juguete, hacemos una foto para saber cómo estaba.

Necesitamos conocer:

- repositorio;
- rama;
- commit;
- cambios pendientes.

### Ejecuta

Ajusta la ruta a la ubicación real:

```powershell
$Repo = "D:\Leovinci\Proyectos LC\LC-IA_0.2"
Set-Location $Repo
```

Registra el estado:

```powershell
@(
    "Fecha: $(Get-Date -Format o)"
    "Repositorio: $(git rev-parse --show-toplevel)"
    "Rama: $(git branch --show-current)"
    "Commit: $(git rev-parse HEAD)"
    ""
    "Estado:"
    (git status --short --branch)
) | Tee-Object -FilePath (Join-Path $EvidenceRoot "01-baseline.txt")
```

### Qué significa cada dato

- **Rama:** línea de trabajo actual.
- **Commit:** fotografía exacta del código.
- **Working tree:** archivos modificados todavía no confirmados.

### Criterio de seguridad

Ejecuta:

```powershell
git status --porcelain
```

- Si no devuelve nada, el repositorio está limpio.
- Si devuelve archivos, anótalos antes de seguir.
- No borres ni descartes nada sin entender de quién es.

### Continúa cuando

Puedas responder:

```text
Estoy en la rama:
Estoy en el commit:
El repositorio está limpio / contiene estos cambios:
```

---

## Paso 3. Localizar las piezas reales

### Idea sencilla

No asumimos dónde está cada pieza. Primero abrimos la caja y miramos.

### Ejecuta

```powershell
$Change = Join-Path $Repo "openspec\changes\synthetic-document-retrieval-foundation"

Get-ChildItem $Change -Recurse |
    Sort-Object FullName |
    Select-Object FullName |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "02-openspec-tree.txt")
```

Busca el módulo Maven:

```powershell
$PomCandidates = Get-ChildItem $Repo -Recurse -Filter pom.xml |
    Where-Object { $_.FullName -match "synthetic-retrieval" }

$PomCandidates |
    Select-Object FullName |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "03-pom-candidates.txt")
```

Selecciona el correcto:

```powershell
$Pom = ($PomCandidates | Select-Object -First 1).FullName
$Pom
```

Busca los archivos Java principales:

```powershell
Get-ChildItem $Repo -Recurse -File |
    Where-Object {
        $_.Name -in @(
            "SyntheticRetrieval.java",
            "InMemorySimulation.java",
            "RetrievalModel.java",
            "SyntheticRetrievalScenarioTest.java"
        )
    } |
    Select-Object Name, FullName |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "04-java-files.txt")
```

### Continúa cuando

Hayas localizado las rutas exactas de:

- cambio OpenSpec;
- `pom.xml`;
- modelo;
- operación;
- simulación;
- suite de escenarios.

---

## Paso 4. Leer OpenSpec como si fuera un cuento

### Idea sencilla

No empieces por el final. Lee el cambio en el orden en que se tomó la decisión:

```text
problema
→ propuesta
→ reglas
→ diseño
→ tareas
→ verificación
```

### Orden de lectura

1. `exploration.md`
2. `proposal.md`
3. archivos de `specs/`
4. `design.md`
5. `tasks.md`
6. `verify-report.md`

### Preguntas para `exploration.md`

- ¿Qué problema intenta resolver?
- ¿Qué alternativas se rechazaron?
- ¿Qué riesgos se querían evitar?
- ¿Qué cosas quedaron fuera?

### Preguntas para `proposal.md`

- ¿Cuál es el objetivo exacto?
- ¿Qué entra?
- ¿Qué no entra?
- ¿Cómo se sabe que el cambio tuvo éxito?

### Preguntas para `specs/`

Para cada requisito:

- ¿Qué comportamiento obliga?
- ¿Qué comportamiento prohíbe?
- ¿Qué escenarios lo demuestran?
- ¿Qué dato debe permanecer secreto?

### Preguntas para `design.md`

- ¿Qué componente tiene cada responsabilidad?
- ¿Qué orden debe seguir el flujo?
- ¿Qué estados deben ser imposibles?
- ¿Cómo se consigue determinismo?

### Preguntas para `tasks.md`

- ¿Cada tarea apunta a un requisito?
- ¿Cada tarea tiene test?
- ¿Hay tareas marcadas que no estén demostradas?

### Evidencia

Crea un archivo de notas fuera del repositorio:

```powershell
$Notes = Join-Path $EvidenceRoot "05-reading-notes.md"

@'
# Notas de lectura OpenSpec

## Exploration
- Problema:
- Alternativas:
- Riesgos:
- Fuera de alcance:

## Proposal
- Objetivo:
- Incluido:
- Excluido:
- Criterios de éxito:

## Specs
- Número de requisitos:
- Número de escenarios:
- Invariantes principales:

## Design
- Flujo:
- Componentes:
- Estados imposibles:
- Determinismo:

## Tasks
- Completadas:
- Dudas:

## Verify report
- Evidencia:
- Pendientes:
'@ | Set-Content $Notes
```

Abre las notas:

```powershell
code $Notes
```

> Este archivo está fuera del repositorio. Puedes escribir libremente sin modificar el proyecto.

---

## Paso 5. Contar requisitos y escenarios de verdad

### Idea sencilla

Si hay treinta preguntas en un examen, una tabla honesta debe tener treinta filas.

No confíes en números escritos en mensajes anteriores. Cuenta los encabezados reales.

### Ejecuta

```powershell
$SpecFiles = Get-ChildItem (Join-Path $Change "specs") -Recurse -Filter *.md

$RequirementMatches = $SpecFiles |
    Select-String -Pattern '^\s*#{1,6}\s+(Requirement|Requisito)\s*:'

$ScenarioMatches = $SpecFiles |
    Select-String -Pattern '^\s*#{1,6}\s+(Scenario|Escenario)\s*:'

@(
    "Archivos de especificación: $($SpecFiles.Count)"
    "Requisitos encontrados: $($RequirementMatches.Count)"
    "Escenarios encontrados: $($ScenarioMatches.Count)"
    ""
    "Requisitos:"
    ($RequirementMatches | ForEach-Object {
        "$($_.Path):$($_.LineNumber) $($_.Line.Trim())"
    })
    ""
    "Escenarios:"
    ($ScenarioMatches | ForEach-Object {
        "$($_.Path):$($_.LineNumber) $($_.Line.Trim())"
    })
) | Tee-Object -FilePath (Join-Path $EvidenceRoot "06-spec-count.txt")
```

### Resultado esperado

```text
Escenarios encontrados: 30
```

El número de requisitos debe salir del contenido real. Si aparece una diferencia entre 12 y 13, no elijas el número que “suena mejor”: abre los archivos y explica la causa.

### Continúa cuando

Puedas demostrar con rutas y líneas:

- cuántos requisitos existen;
- que existen treinta escenarios.

---

## Paso 6. Revisar la matriz de trazabilidad

### Idea sencilla

Una matriz de trazabilidad es una lista que une:

```text
regla del plano
→ prueba que la comprueba
```

Un mismo test puede comprobar varias reglas. Eso no es malo, pero debe quedar claro.

### Añade estas columnas

| # | Escenario | Test | Tipo de cobertura | Resultado | Evidencia | Observaciones |
|---:|---|---|---|---|---|---|

Usa estos valores en `Tipo de cobertura`:

- **Directa:** el test está centrado en ese escenario.
- **Compartida:** un mismo test comprueba varios escenarios.
- **Adversarial:** el test intenta romper una protección.
- **Estructural:** el tipo o contrato impide construir el estado inválido.

### Regla importante

No marques un escenario como cubierto solo porque el nombre del test se parece.

Abre el test y comprueba tres cosas:

1. prepara la situación descrita;
2. ejecuta el comportamiento correcto;
3. contiene una aserción que fallaría si la regla no se cumpliera.

---

# Parte II — Entender el código desde lo más sencillo

## Paso 7. Leer primero el `pom.xml`

### Idea sencilla

El `pom.xml` es la lista de ingredientes y herramientas del módulo.

### Ejecuta

```powershell
Get-Content $Pom |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "07-pom.txt")
```

### Busca

- versión de Java;
- JUnit;
- plugins Maven;
- nombre del módulo;
- configuración de tests;
- dependencias externas.

### Preguntas

- ¿El módulo depende de Spring?
- ¿Depende de una base de datos?
- ¿Usa red?
- ¿Usa filesystem real?
- ¿Solo necesita Java y JUnit?

### Conclusión esperada

El núcleo sintético debería ser pequeño y estar desacoplado de infraestructura productiva.

---

## Paso 8. Leer `RetrievalModel.java`: los nombres del mundo

### Idea sencilla

El modelo contiene los “sustantivos”:

- actor;
- tenant;
- scope;
- gateway;
- candidato;
- cobertura;
- decisión;
- traza.

Antes de entender lo que hace el sistema, necesitas saber qué cosas existen.

### Método de lectura

Para cada `record`, `class`, `enum` o `interface`, anota:

```text
Nombre:
Qué representa:
Qué datos contiene:
Qué combinaciones permite:
Qué combinaciones debería prohibir:
```

### Conceptos que debes localizar

- procedencia sintética;
- contexto autorizado;
- intención;
- scope elegible o resuelto;
- snapshot;
- gateway obligatorio u opcional;
- contribución;
- cobertura `COMPLETE` o `PARTIAL`;
- candidato;
- decisión;
- outcome;
- traza.

### Pregunta esencial

> ¿El propio tipo impide crear estados inválidos o confía en que quien lo usa “se porte bien”?

Si un `DENIED` puede construirse con candidatos y cobertura, el modelo es débil.  
Si existe un tipo específico de denegación que no tiene esos campos, el modelo es más seguro.

---

## Paso 9. Leer `SyntheticRetrieval.java`: los verbos del mundo

### Idea sencilla

Aquí viven las acciones:

```text
comprobar
→ autorizar
→ elegir
→ congelar
→ consolidar
→ decidir
→ trazar
```

### Encuentra la operación end-to-end

Busca métodos públicos:

```powershell
$SyntheticFile = Get-ChildItem $Repo -Recurse -Filter SyntheticRetrieval.java |
    Select-Object -First 1

Select-String -Path $SyntheticFile.FullName `
    -Pattern 'public\s+.*\(' `
    -Context 0,2
```

### Dibuja el flujo real

No copies todavía el diseño. Dibuja lo que hace el código:

```text
entrada
  ↓
...
  ↓
resultado
```

### Verifica el orden

La operación debería obligar a pasar por:

```text
guard
→ autorización
→ intención
→ scope
→ snapshot
→ consolidación
→ outcome
→ trace
```

### Preguntas

- ¿Puede llegarse a consolidación sin autorización?
- ¿Puede resolverse el scope antes de validar tenant?
- ¿El snapshot se crea antes de recibir o consolidar contribuciones?
- ¿La traza se construye desde datos permitidos?
- ¿Hay una salida temprana para `DENIED`?

---

## Paso 10. Leer `InMemorySimulation.java`: el pequeño mundo de juguete

### Idea sencilla

La simulación crea actores, tenants, scopes y gateways falsos para probar reglas reales.

Es como una ciudad de juguete:

- las casas son falsas;
- las normas de tráfico que se prueban son reales.

### Busca

- tenants sintéticos;
- actores;
- memberships;
- grants;
- sources;
- collections;
- gateways;
- candidatos;
- contribuciones externas.

### Preguntas

- ¿Cómo se distingue `tenant-a` de `tenant-b`?
- ¿Cómo se representa un grant revocado?
- ¿Cómo se marca un gateway como obligatorio?
- ¿Cómo se crea una contribución de otro tenant?
- ¿Los fixtures contienen contenido sensible para probar minimización?

> **Fixture** significa conjunto de datos preparado para una prueba.

---

# Parte III — Seguir una ejecución como detective

## Paso 11. Seguir el caso feliz

Usa este test:

```text
endToEndOperationUsesAuthorizedIntentToSelectAnEligibleSourceScope
```

### Ejecuta solo ese test

```powershell
mvn -f $Pom `
    "-Dtest=SyntheticRetrievalScenarioTest#endToEndOperationUsesAuthorizedIntentToSelectAnEligibleSourceScope" `
    test 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "08-test-happy-path.txt")
```

### Lee el test en cuatro bloques

1. **Preparación:** qué actor, tenant, grant e intención crea.
2. **Acción:** qué método ejecuta.
3. **Resultado:** qué devuelve.
4. **Aserciones:** qué reglas comprueba.

### Cuaderno de detective

```text
Actor:
Tenant:
Grant:
Intención:
Scopes posibles:
Scope elegido:
Por qué se eligió:
Resultado:
Traza:
```

### Lo que debes aprender

El scope no se elige solo porque sea pequeño. Debe ser:

- autorizado;
- compatible con la intención;
- elegible;
- el menor que satisface la intención.

---

## Paso 12. Seguir una denegación segura

Usa este test:

```text
endToEndOperationEmitsOnlyTheSafeDeniedTraceForInvalidAuthorization
```

### Ejecuta

```powershell
mvn -f $Pom `
    "-Dtest=SyntheticRetrievalScenarioTest#endToEndOperationEmitsOnlyTheSafeDeniedTraceForInvalidAuthorization" `
    test 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "09-test-denied.txt")
```

### Preguntas

- ¿En qué momento se detiene?
- ¿Se crea snapshot?
- ¿Se calculan candidatos?
- ¿Aparece cobertura?
- ¿La traza revela IDs, nombres o fuentes?
- ¿Qué motivo seguro queda registrado?

### Regla que debes poder explicar

> Si la persona no tiene permiso, el sistema debe cerrar la puerta sin enseñar qué hay detrás.

---

## Paso 13. Seguir una cobertura parcial

Primero ejecuta:

```text
missingGatewaysRemainPartialUnavailableWhilePreservingCandidates
```

```powershell
mvn -f $Pom `
    "-Dtest=SyntheticRetrievalScenarioTest#missingGatewaysRemainPartialUnavailableWhilePreservingCandidates" `
    test 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "10-test-partial.txt")
```

Después ejecuta:

```text
requiredGatewayAbsenceEscalatesAnOtherwiseAvailableResult
```

```powershell
mvn -f $Pom `
    "-Dtest=SyntheticRetrievalScenarioTest#requiredGatewayAbsenceEscalatesAnOtherwiseAvailableResult" `
    test 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "11-test-required-gateway.txt")
```

### Compara

| Pregunta | Gateway opcional ausente | Gateway obligatorio ausente |
|---|---|---|
| ¿Cobertura completa? | No | No |
| ¿Resultado `PARTIAL`? | Sí | Sí |
| ¿Puede conservar candidatos? | Sí | Sí |
| ¿La ausencia obliga a escalar? | No siempre | Sí |

### Analogía

- Gateway opcional: falta un alumno que podía ayudar.
- Gateway obligatorio: falta el profesor que debía validar el examen.

En ambos casos falta alguien, pero la consecuencia no es la misma.

---

## Paso 14. Seguir ambigüedad, evidencia insuficiente y obsolescencia

Usa:

```text
choosesOnlyConservativeDecisionsAndLimitsNotLocatedToCompleteScope
```

### Ejecuta

```powershell
mvn -f $Pom `
    "-Dtest=SyntheticRetrievalScenarioTest#choosesOnlyConservativeDecisionsAndLimitsNotLocatedToCompleteScope" `
    test 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "12-test-conservative-decisions.txt")
```

### Debes distinguir

- **AMBIGUOUS:** hay varias respuestas posibles.
- **INSUFFICIENT:** falta información para decidir.
- **STALE:** el candidato es una versión antigua.
- **NOT_LOCATED_IN_SCOPE:** no se encontró dentro de un ámbito completamente consultado.

### Regla esencial

```text
cero candidatos + cobertura PARTIAL
≠
NOT_LOCATED_IN_SCOPE
```

Solo se permite `NOT_LOCATED_IN_SCOPE` cuando la cobertura necesaria fue completa.

---

## Paso 15. Seguir aislamiento y determinismo

Usa:

```text
keepsCandidateAndTraceOrderStableAndExcludesCrossTenantContributions
```

### Ejecuta

```powershell
mvn -f $Pom `
    "-Dtest=SyntheticRetrievalScenarioTest#keepsCandidateAndTraceOrderStableAndExcludesCrossTenantContributions" `
    test 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "13-test-determinism-tenant.txt")
```

### Busca dos pruebas distintas

#### Aislamiento

Una contribución de otro tenant:

- no entra;
- no cambia el resultado;
- no cambia el orden;
- no aparece en la traza;
- no permite inferir que existe.

#### Determinismo

Las mismas entradas lógicas producen:

- los mismos candidatos;
- en el mismo orden;
- la misma traza;
- la misma decisión.

### Analogía

Si barajas las hojas antes de entregarlas, el profesor debe obtener la misma nota final. El orden accidental de llegada no debe cambiar la verdad.

---

## Paso 16. Seguir los estados imposibles

Ejecuta:

```text
rejectsInvalidNotLocatedCombinations
```

```powershell
mvn -f $Pom `
    "-Dtest=SyntheticRetrievalScenarioTest#rejectsInvalidNotLocatedCombinations" `
    test 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "14-test-invalid-not-located.txt")
```

Después:

```text
traceOnlyCarriesAllowedOpaqueDimensionsAndRejectsInvalidState
```

```powershell
mvn -f $Pom `
    "-Dtest=SyntheticRetrievalScenarioTest#traceOnlyCarriesAllowedOpaqueDimensionsAndRejectsInvalidState" `
    test 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "15-test-invalid-trace.txt")
```

### Preguntas

- ¿Qué combinación inválida intenta crear el test?
- ¿La rechaza el constructor, una factory o una validación?
- ¿Qué dimensiones permite la traza?
- ¿Qué datos prohíbe?
- ¿El error ocurre antes de devolver el outcome?

### Objetivo profundo

Poder explicar:

> El código no solo conoce los estados correctos; también impide representar ciertos estados incorrectos.

---

# Parte IV — Demostrar el conjunto completo

## Paso 17. Ejecutar la suite completa

### Ejecuta

```powershell
mvn -f $Pom clean test 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "16-mvn-clean-test.txt")

if ($LASTEXITCODE -ne 0) {
    throw "La suite Maven ha fallado."
}
```

### Resultado esperado

```text
Tests run: 30
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

### Importante

Maven puede crear `target/`. Eso es un artefacto de compilación, no una modificación funcional del código. Normalmente está ignorado por Git.

---

## Paso 18. Inspeccionar el reporte de Surefire

### Idea sencilla

La consola dice “aprobado”. El XML de Surefire es el acta del examen.

### Ejecuta

```powershell
$ModuleDir = Split-Path $Pom
$Surefire = Join-Path $ModuleDir "target\surefire-reports"

Get-ChildItem $Surefire -Filter "TEST-*.xml" |
    ForEach-Object {
        [xml]$Xml = Get-Content $_.FullName

        [pscustomobject]@{
            File     = $_.Name
            Tests    = [int]$Xml.testsuite.tests
            Failures = [int]$Xml.testsuite.failures
            Errors   = [int]$Xml.testsuite.errors
            Skipped  = [int]$Xml.testsuite.skipped
            Time     = $Xml.testsuite.time
        }
    } |
    Format-Table -AutoSize |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "17-surefire-summary.txt")
```

### Continúa cuando

El resumen confirme treinta ejecuciones y cero fallos.

---

## Paso 19. Empaquetar el módulo

### Idea sencilla

Los tests comprueban reglas. `package` comprueba además que Maven puede construir el paquete final.

### Ejecuta

```powershell
mvn -f $Pom package 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "18-mvn-package.txt")

if ($LASTEXITCODE -ne 0) {
    throw "El empaquetado Maven ha fallado."
}
```

Muestra el artefacto:

```powershell
Get-ChildItem (Join-Path $ModuleDir "target") -Filter *.jar |
    Select-Object Name, Length, LastWriteTime |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "19-package-artifact.txt")
```

---

## Paso 20. Comprobar que Git no detecta daños

### Ejecuta

```powershell
git diff --check 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "20-git-diff-check.txt")

git status --short --branch |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "21-git-status-after-tests.txt")
```

### Sobre LF y CRLF

- **LF:** final de línea habitual en Linux.
- **CRLF:** final de línea habitual en Windows.

Un aviso de conversión no significa por sí mismo que el código esté mal.  
`git diff --check` debe terminar correctamente.

---

## Paso 21. Proteger `verify-report.md` con una huella

### Idea sencilla

Una huella SHA-256 es como la huella digital de un archivo. Si cambia un solo carácter, cambia la huella.

### Ejecuta antes de modificar documentación

```powershell
$VerifyReport = Get-ChildItem $Change -Recurse -Filter verify-report.md |
    Select-Object -First 1

$VerifyBefore = Get-FileHash $VerifyReport.FullName -Algorithm SHA256

$VerifyBefore |
    Format-List |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "22-verify-report-hash-before.txt")
```

Si vuelves a comprobarlo sin haber editado:

```powershell
$VerifyAfter = Get-FileHash $VerifyReport.FullName -Algorithm SHA256

$VerifyBefore.Hash -eq $VerifyAfter.Hash
```

Resultado esperado:

```text
True
```

---

# Parte V — Resolver el único pendiente conocido

## Paso 22. Investigar el intento de afirmación factual

### El problema explicado de forma sencilla

Encontrar un documento no es lo mismo que demostrar una frase sobre su contenido.

Ejemplo:

```text
Documento encontrado:
Contrato de mantenimiento.

Afirmación factual:
El contrato vence el 31 de diciembre.
```

La primera tarea es localizar el documento.  
La segunda exigiría leer y demostrar un hecho concreto.

El incremento actual debe impedir que una recuperación documental se convierta silenciosamente en una respuesta factual.

### Busca campos o estructuras abiertas

Con `rg`:

```powershell
rg -n `
    "factual|claim|assertion|answer|payload|metadata|trace|Map<|Object" `
    $ModuleDir |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "23-factual-claim-search.txt")
```

Sin `rg`:

```powershell
Get-ChildItem $ModuleDir -Recurse -Include *.java |
    Select-String `
        -Pattern "factual|claim|assertion|answer|payload|metadata|trace|Map<|Object" |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "23-factual-claim-search.txt")
```

### Decide entre dos casos

#### Caso A — Exclusión estructural suficiente

Se cumple si:

- el resultado usa tipos cerrados;
- la traza solo admite dimensiones concretas;
- no existe `Map<String, Object>` ni metadata libre;
- no existe campo para respuesta factual;
- un consumidor no puede introducir arbitrariamente una afirmación.

Conclusión posible:

> La afirmación factual está fuera del contrato y no puede representarse. Falta una prueba runtime independiente, pero existe exclusión estructural.

Esto puede documentarse como riesgo residual o endurecimiento posterior si ninguna spec exige una prueba runtime concreta.

#### Caso B — Existe una puerta genérica

Se cumple si:

- la traza acepta claves libres;
- hay un mapa genérico;
- puede pasar un payload arbitrario;
- un consumidor puede colocar `factual_claim`, `answer` o contenido.

Conclusión:

> El contrato permite transportar una dimensión no autorizada.

En ese caso añade primero un test RED, por ejemplo:

```text
rejectsFactualClaimDimensionFromTrace
```

El test debería demostrar que una dimensión como:

```text
factual_claim
answer
document_content
extracted_value
```

es rechazada o eliminada de forma segura.

### Regla importante

No añadas un campo `factualClaim` solo para poder rechazarlo. Eso ampliaría el modelo con una capacidad fuera del incremento.

---

# Parte VI — Alinear documentación sin mezclar funcionalidad

## Paso 23. Crear una rama solo para el cierre documental

No hagas este paso hasta terminar toda la observación anterior.

### Ejecuta

```powershell
git switch -c docs/synthetic-retrieval-verification-alignment
```

### Propósito

Esta rama debe contener únicamente:

- corrección del estado OpenSpec;
- evidencia de verificación;
- matriz de treinta escenarios;
- criterios de éxito;
- clasificación del pendiente factual.

No debe contener:

- Spring;
- API;
- PostgreSQL;
- Auth0;
- gateway real;
- refactor general;
- nuevas capacidades.

---

## Paso 24. Actualizar la fuente de verdad

Revisa y modifica únicamente los archivos que realmente estén obsoletos.

### `config.yaml`

Corrige afirmaciones como:

```text
no existe implementación
no existe test runner
```

La nueva redacción debe reflejar exactamente:

```text
Existe una implementación sintética Java en memoria.
Existe una suite Maven/JUnit.
La validación actual confirma treinta tests correctos y package correcto.
No existe integración productiva con documentos reales.
```

### `proposal.md`

Marca únicamente los criterios de éxito demostrados.

No marques:

- integración real;
- producción;
- documentos reales;
- afirmaciones factuales, si siguen fuera o pendientes.

### `verify-report.md`

Registra:

- fecha;
- commit;
- comandos exactos;
- treinta tests;
- cero fallos;
- `package` correcto;
- `git diff --check`;
- matriz de treinta escenarios;
- pendiente factual;
- riesgos residuales.

### Matriz

No la reduzcas a veintiocho filas.

Añade:

- tipo de cobertura;
- evidencia;
- observaciones;
- estado.

---

## Paso 25. Revisar el diff como un revisor independiente

### Ejecuta

```powershell
git diff --stat
git diff --check
git diff
```

### Preguntas

- ¿Solo cambié documentación y configuración?
- ¿Alguna frase afirma más de lo demostrado?
- ¿Los treinta escenarios siguen presentes?
- ¿El pendiente factual está visible?
- ¿Dije “productivo” en algún sitio sin ser verdad?
- ¿Dije que existe `UNEQUIVOCAL`?
- ¿Confundí un test compartido con una prueba directa?

---

## Paso 26. Ejecutar la verificación final después de editar

```powershell
mvn -f $Pom clean test 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "24-final-test.txt")

mvn -f $Pom package 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "25-final-package.txt")

git diff --check 2>&1 |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "26-final-diff-check.txt")

git status --short --branch |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "27-final-status.txt")
```

### Gate de salida

No continúes si:

- falla un test;
- falla el empaquetado;
- la matriz no contiene treinta escenarios;
- una spec no tiene evidencia;
- el pendiente factual está oculto;
- la documentación contradice el código.

---

## Paso 27. Crear un commit pequeño y explicativo

### Revisa primero

```powershell
git diff --name-only
```

### Añade únicamente los archivos de cierre

```powershell
git add openspec
git status --short
```

### Commit sugerido

```powershell
git commit -m "docs(openspec): align synthetic retrieval verification evidence"
```

### Registra la evidencia

```powershell
git show --stat --oneline HEAD |
    Tee-Object -FilePath (Join-Path $EvidenceRoot "28-verification-commit.txt")
```

---

# Parte VII — Decidir si el cambio puede archivarse

## Paso 28. Aplicar el gate de archivo

El flujo del proyecto es:

```text
explore
→ proposal
→ spec
→ design
→ tasks
→ apply
→ verify
→ archive
```

Antes de archivar, responde sí o no:

- [ ] La implementación actual está descrita correctamente.
- [ ] Los treinta escenarios están mapeados.
- [ ] Los treinta tests pasan.
- [ ] Maven empaqueta el módulo.
- [ ] No existen estados semánticos inválidos conocidos.
- [ ] La ausencia de gateway obligatorio escala.
- [ ] Los cruces de tenant no son observables.
- [ ] La traza está minimizada.
- [ ] `UNEQUIVOCAL` sigue fuera del dominio.
- [ ] El pendiente factual está clasificado.
- [ ] Los riesgos residuales están escritos.
- [ ] No se afirma que exista un buscador productivo.

### Importante

Los documentos disponibles explican **qué debe significar `archive`**, pero no permiten deducir con seguridad el mecanismo físico exacto que usa tu instalación concreta de OpenSpec.

Por tanto:

1. comprueba si existe un CLI nativo:

```powershell
Get-Command openspec -ErrorAction SilentlyContinue
openspec --help
```

2. busca instrucciones locales:

```powershell
Get-ChildItem $Repo -Recurse -File |
    Where-Object {
        $_.Name -match "README|AGENTS|CONTRIBUTING" -or
        $_.FullName -match "openspec"
    } |
    Select-String -Pattern "archive|verify|openspec" |
    Select-Object Path, LineNumber, Line
```

3. no inventes un `git mv` ni una estructura de archivo sin confirmar la convención.

El defecto circular de Gentle AI no debe obligarte a falsificar un archivo. Puedes dejar el cambio **verificado manualmente y preparado para archivar**, con toda la evidencia, hasta confirmar el mecanismo nativo correcto.

---

# Parte VIII — Qué construir después del cierre

Cuando el cambio esté cerrado, no saltes a toda la infraestructura.

El siguiente incremento debería ser uno solo y vertical.

Orden recomendado:

1. conservar la operación sintética end-to-end;
2. cerrar el pendiente factual si realmente bloquea;
3. elegir un único borde real:
   - identidad;
   - API;
   - persistencia;
   - gateway;
4. mantener el resto simulado;
5. crear nuevas specs y tests antes de implementar.

Ejemplo seguro:

```text
Actor real autenticado
→ operación de recuperación todavía sintética
→ resultado y traza sintéticos
```

O:

```text
Actor sintético
→ un gateway real
→ resto del flujo sintético
```

No construir simultáneamente:

```text
Auth0 + API + PostgreSQL + OCR + filesystem + gateway + red
```

porque, si algo falla, no sabrás qué pieza produjo el error.

---

# Anexo A — Matriz de treinta escenarios

| # | Escenario | Test |
|---:|---|---|
| 1 | Entrada no sintética | `rejectsNonSyntheticProvenanceBeforeInspectingFixtures` |
| 2 | Selección concluyente | `endToEndOperationUsesAuthorizedIntentToSelectAnEligibleSourceScope` |
| 3 | Snapshot congelado | `snapshotFreezesResolvedScopeAndGatewayConfigurationBeforeConsolidation` |
| 4 | Cobertura completa | `snapshotDefensivelyFreezesGatewaysBeforeFixtureMutation` |
| 5 | Cobertura parcial | `missingGatewaysRemainPartialUnavailableWhilePreservingCandidates` |
| 6 | Gateway obligatorio ausente | `requiredGatewayAbsenceEscalatesAnOtherwiseAvailableResult` |
| 7 | Gateway opcional ausente | `missingGatewaysRemainPartialUnavailableWhilePreservingCandidates` |
| 8 | Candidatos ambiguos | `choosesOnlyConservativeDecisionsAndLimitsNotLocatedToCompleteScope` |
| 9 | Evidencia insuficiente | `choosesOnlyConservativeDecisionsAndLimitsNotLocatedToCompleteScope` |
| 10 | Referencia stale | `choosesOnlyConservativeDecisionsAndLimitsNotLocatedToCompleteScope` |
| 11 | No localizado en ámbito | `endToEndOperationSelectsTheSmallestFullyAuthorizedCollectionForTheIntent` |
| 12 | Combinación inválida | `rejectsInvalidNotLocatedCombinations` |
| 13 | Orden y cruce adversarial | `keepsCandidateAndTraceOrderStableAndExcludesCrossTenantContributions` |
| 14 | Contexto autorizado | `resolvesOnlyGrantedScopesWithinTheActiveTenant` |
| 15 | Contexto/grant inválido | `deniesInvalidContextAndRevokedGrantWithoutDetails` |
| 16 | Source scope elegible | `endToEndOperationUsesAuthorizedIntentToSelectAnEligibleSourceScope` |
| 17 | Collection autorizado | `collectionRequiresGrantsForOptionalSourcesAndDeduplicatesSharedSources` |
| 18 | Grant opcional ausente | `endToEndOperationDoesNotTreatAnUnauthorizedCollectionAsAnAuthorizedPartialView` |
| 19 | Ámbito mínimo | `endToEndOperationSelectsTheSmallestFullyAuthorizedCollectionForTheIntent` |
| 20 | Ámbitos equivalentes | `endToEndOperationEmitsAnAuthorizedClarificationTraceForEquivalentScopes` |
| 21 | Repetición estable | `endToEndOperationProducesDeterministicClarificationForTheSameAuthorizedIntent` |
| 22 | Cruce de tenant | `crossTenantEntitiesNeverEnterResolutionOrChangeTheSafeResult` |
| 23 | Resultado multidimensional | `keepsCandidateAndTraceOrderStableAndExcludesCrossTenantContributions` |
| 24 | Aclaración de ámbito | `endToEndOperationEmitsAnAuthorizedClarificationTraceForEquivalentScopes` |
| 25 | Carga sensible | `clarificationTraceDoesNotExposeFixturePayloads` |
| 26 | Prohibición normativa | `excludesUnequivocalFromDecisionDomain` |
| 27 | Contribución externa | `keepsCandidateAndTraceOrderStableAndExcludesCrossTenantContributions` |
| 28 | Denegación minimizada | `endToEndOperationEmitsOnlyTheSafeDeniedTraceForInvalidAuthorization` |
| 29 | Repetición determinista | `keepsCandidateAndTraceOrderStableAndExcludesCrossTenantContributions` |
| 30 | Estado semántico inválido | `traceOnlyCarriesAllowedOpaqueDimensionsAndRejectsInvalidState` |

---

# Anexo B — Plantilla para estudiar un test

```markdown
## Test

`nombreDelTest`

### Regla que pretende demostrar

...

### Preparación

- Actor:
- Tenant:
- Membership:
- Grants:
- Intención:
- Scopes:
- Gateways:
- Contribuciones:

### Acción

...

### Resultado esperado

...

### Aserciones importantes

1. ...
2. ...
3. ...

### Qué ocurriría si el código estuviera mal

...

### Evidencia

- Comando:
- Resultado:
- Archivo guardado:

### Dudas

- ...
```

---

# Anexo C — Checklist final de aprendizaje

Debes poder explicar con tus propias palabras:

- [ ] Qué diferencia existe entre OpenSpec y `synthetic-retrieval`.
- [ ] Por qué el sistema todavía no es un buscador productivo.
- [ ] Qué protege el guard sintético.
- [ ] Qué diferencia hay entre membership y grant.
- [ ] Cómo usa la intención para elegir scope.
- [ ] Por qué el snapshot debe crearse antes de consolidar.
- [ ] Qué diferencia hay entre `COMPLETE` y `PARTIAL`.
- [ ] Por qué falta de gateway obligatorio siempre escala.
- [ ] Por qué un candidato único no implica certeza.
- [ ] Qué significa `STALE`.
- [ ] Cuándo se permite `NOT_LOCATED_IN_SCOPE`.
- [ ] Cómo se evita mezclar tenants.
- [ ] Qué significa determinismo.
- [ ] Qué información puede transportar la traza.
- [ ] Por qué localizar un documento no permite afirmar hechos sobre él.
- [ ] Qué prueban los treinta tests.
- [ ] Qué falta para archivar de forma honesta.

---

## Cierre de la sesión de evidencia

Cuando termines:

```powershell
Stop-Transcript
```

Después conserva la carpeta `$EvidenceRoot` junto con:

- commit analizado;
- salidas Maven;
- resumen Surefire;
- matriz;
- notas;
- decisión sobre el pendiente factual.
