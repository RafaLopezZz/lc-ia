# LC-IA — M0.4 Architecture Guards con ArchUnit

- **Estado:** M0.4 implementado, verificado y archivado
- **Proyecto:** LC-IA / Plataforma de Leos
- **Módulo:** `lc-ia-server`
- **Rama de trabajo:** `test/m0-4-architecture-guards`
- **Tecnología:** Java 25, Spring Boot 4.1.0, Maven, JUnit 5, ArchUnit 1.5.0

---

## 1. Qué problema resuelve M0.4

La arquitectura de LC-IA establece una dirección de dependencias propia de un monolito modular con arquitectura hexagonal:

```text
Dominio
  ↑
Aplicación / casos de uso
  ↑
Puertos
  ↑
Adaptadores
```

Esto implica, entre otras cosas, que:

- el dominio no debe depender de Spring;
- el dominio no debe depender de JPA/Hibernate;
- el dominio no debe depender de adaptadores;
- un controlador no debe saltarse la capa de aplicación y depender directamente de un adaptador concreto de persistencia.

La documentación por sí sola no puede impedir que, dentro de unos meses, alguien escriba accidentalmente algo como:

```java
package com.leovinci.leos.document.domain;

import org.springframework.context.ApplicationEventPublisher;

public class Document {
    private final ApplicationEventPublisher publisher;
}
```

El compilador Java lo aceptaría. Spring también podría arrancar. El problema es arquitectónico.

**M0.4 convierte esas restricciones en tests automáticos.**

---

## 2. Qué es ArchUnit en este contexto

ArchUnit analiza las clases Java compiladas y permite formular reglas sobre su estructura y sus dependencias.

No ejecuta el dominio ni necesita levantar Spring para comprobar estas reglas. Inspecciona el bytecode y construye un modelo de las clases: paquetes, campos, métodos, anotaciones, herencia y dependencias entre tipos.

Por ejemplo, esta relación:

```java
private final ApplicationEventPublisher publisher;
```

produce una dependencia observable:

```text
NuestraClase → org.springframework.context.ApplicationEventPublisher
```

ArchUnit puede detectar esa relación y rechazarla si contradice una regla arquitectónica.

---

## 3. Por qué no bastaba con escribir las reglas directamente

Cuando empezamos M0.4, `lc-ia-server` prácticamente solo tenía la clase de arranque:

```text
src/main/java
└── com/leovinci/leos/LcIaApplication.java
```

Todavía no había clases reales en paquetes `domain`, controladores ni adaptadores.

Una regla como:

```text
ninguna clase domain puede depender de Spring
```

podría pasar simplemente porque hay **cero clases de dominio**.

Eso se denomina aquí un **verde vacío o vacuous pass**: la regla no encuentra una violación porque todavía no existe ninguna clase a la que aplicarla.

Por eso M0.4 necesita dos niveles distintos de prueba:

1. **Fixtures controladas en `src/test/java`** que violan deliberadamente cada regla y demuestran que el detector funciona.
2. **Evaluación de `target/classes`** que aplica esas mismas reglas al código real de producción.

La combinación es importante:

```text
Fixture incorrecta
      ↓
la guarda DEBE detectarla
      ↓
prueba del detector

Producción
      ↓
la misma guarda NO debe encontrar violaciones
      ↓
protección real del código
```

---

## 4. Por qué las fixtures están en `src/test/java`

Una fixture es una clase creada únicamente para probar una condición arquitectónica.

Ejemplo:

```text
src/test/java/
└── com/leovinci/leos/architecturefixtures/domain/spring/
    └── DomainDependingOnSpring.java
```

La fixture representa deliberadamente un diseño que **no queremos en producción**.

No debe colocarse en `src/main/java`, porque entonces estaríamos contaminando la arquitectura productiva con código falso únicamente para satisfacer una prueba.

Durante el desarrollo se detectó precisamente un error de ubicación: `DomainDependingOnSpring` se creó inicialmente en `main`. Maven lo reveló porque compilaba dos clases de producción y una sola de test. Tras moverla correctamente, la señal pasó a ser:

```text
main compile → 1 clase
test compile → 2 clases
```

Este control resultó útil para confirmar que las fixtures permanecen aisladas.

---

# 5. Preparación del banco de pruebas

## Paso 5.1 — Baseline inicial

Antes de añadir ArchUnit se ejecutó:

```powershell
mvn test
```

Resultado esperado y obtenido:

```text
LcIaApplicationTest → PASS
BUILD SUCCESS
```

### Qué demuestra

Que el módulo estaba verde antes de introducir M0.4. Esto nos da un punto de comparación: cualquier fallo posterior debe proceder del cambio que estamos realizando.

---

## Paso 5.2 — Añadir ArchUnit

Se añadió al `pom.xml`:

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit</artifactId>
    <version>1.5.0</version>
    <scope>test</scope>
</dependency>
```

### Por qué `test` scope

ArchUnit es una herramienta de verificación arquitectónica, no una dependencia necesaria para ejecutar LC-IA en producción.

Por tanto:

```text
producción → no necesita ArchUnit
tests      → sí necesitan ArchUnit
```

### Qué se comprobó después

```powershell
mvn test
```

continuó en verde.

Esto confirmó que:

- ArchUnit 1.5.0 funciona con el toolchain actual;
- Maven/Surefire sigue ejecutando JUnit 5 normalmente;
- no necesitamos `archunit-junit*` para este enfoque;
- la dependencia no altera el runtime productivo.

---

# 6. Slice 1 — Prohibir `domain → Spring`

## Paso 6.1 — Crear una violación controlada

Fixture:

```java
package com.leovinci.leos.architecturefixtures.domain.spring;

import org.springframework.context.ApplicationEventPublisher;

public class DomainDependingOnSpring {

    private final ApplicationEventPublisher publisher;

    public DomainDependingOnSpring(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }
}
```

### Qué representa

Una clase considerada de dominio depende directamente de una clase Spring:

```text
DomainDependingOnSpring
        ↓
ApplicationEventPublisher
        ↓
org.springframework...
```

### Por qué no usamos `@Component`

No necesitamos convertir la fixture en un bean. Solo necesitamos una dependencia clara y visible en bytecode.

El campo y el constructor son suficientes.

---

## Paso 6.2 — Comprobar que la fixture por sí sola no rompe nada

Se ejecutó:

```powershell
mvn test
```

Y siguió verde.

### Qué demuestra

Tener una clase arquitectónicamente incorrecta en bytecode no provoca ningún fallo por sí mismo.

Necesitamos una regla que la inspeccione.

---

## Paso 6.3 — RED: escribir primero la expectativa

Se creó `ArchitectureBoundaryGuardsTest` con:

```java
@Test
void springDomainFixtureIsRejected() {
    JavaClasses fixtureClasses =
            new ClassFileImporter().importClasses(DomainDependingOnSpring.class);

    assertThrows(
            AssertionError.class,
            () -> domainMustNotDependOnSpring().check(fixtureClasses)
    );
}
```

pero todavía **no existía**:

```java
domainMustNotDependOnSpring()
```

La ejecución:

```powershell
mvn test -Dtest=ArchitectureBoundaryGuardsTest
```

produjo el RED esperado:

```text
cannot find symbol
method domainMustNotDependOnSpring()
BUILD FAILURE
```

### Por qué este RED es válido

El test expresa una capacidad que todavía no existe. Primero definimos la expectativa; después implementamos la mínima guarda necesaria.

En este caso el RED fue de compilación. Eso sigue siendo TDD: la interfaz esperada todavía no estaba implementada.

---

## Paso 6.4 — GREEN: implementar la regla mínima

Se añadió:

```java
private ArchRule domainMustNotDependOnSpring() {
    return noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("org.springframework..")
            .allowEmptyShould(true);
}
```

La regla se lee casi como una frase:

```text
ninguna clase
que esté en un paquete ...domain...
debe depender de una clase
en org.springframework...
```

### Por qué `assertThrows` produce un test verde

La fixture es deliberadamente incorrecta.

Queremos esta cadena:

```text
fixture viola la arquitectura
        ↓
ArchUnit lanza AssertionError
        ↓
assertThrows confirma que debía ocurrir
        ↓
JUnit PASS
```

Por tanto, un test verde aquí significa:

> el detector ha rechazado correctamente una violación conocida.

---

# 7. Slice 2 — Prohibir `domain → JPA/Hibernate`

## Paso 7.1 — Añadir Jakarta Persistence solo para tests

Antes de crear la fixture se comprobó el POM efectivo:

```powershell
mvn help:effective-pom | Select-String -Pattern "jakarta.persistence-api" -Context 2,4
```

Spring Boot ya gestionaba:

```text
jakarta.persistence-api 3.2.0
```

Por tanto se añadió sin declarar versión:

```xml
<dependency>
    <groupId>jakarta.persistence</groupId>
    <artifactId>jakarta.persistence-api</artifactId>
    <scope>test</scope>
</dependency>
```

### Por qué no añadimos un starter JPA

Solo necesitamos una API real para construir la violación controlada.

No necesitamos:

- Spring Data JPA;
- Hibernate runtime;
- datasource;
- configuración de persistencia.

Esto mantiene el cambio mínimo.

---

## Paso 7.2 — Fixture JPA

```java
package com.leovinci.leos.architecturefixtures.domain.jpa;

import jakarta.persistence.Entity;

@Entity
public class DomainDependingOnJpa {
}
```

La anotación `@Entity` queda registrada en el bytecode y constituye una dependencia hacia `jakarta.persistence`.

---

## Paso 7.3 — RED

Se escribió primero:

```java
@Test
void jpaOrHibernateDomainFixtureIsRejected() {
    JavaClasses fixtureClasses =
            new ClassFileImporter().importClasses(DomainDependingOnJpa.class);

    assertThrows(
            AssertionError.class,
            () -> domainMustNotDependOnJpaOrHibernate().check(fixtureClasses)
    );
}
```

sin implementar aún el método.

Para evitar que Maven reutilizara bytecode previo con un error no resuelto, se utilizó una ejecución limpia:

```powershell
mvn clean test -Dtest=ArchitectureBoundaryGuardsTest
```

El RED reproducible fue:

```text
cannot find symbol
method domainMustNotDependOnJpaOrHibernate()
BUILD FAILURE
```

### Aprendizaje

Para obtener evidencia fiable de RED/GREEN conviene usar `clean` cuando haya dudas sobre `target/`. Así demostramos que el resultado se reproduce desde bytecode reconstruido.

---

## Paso 7.4 — GREEN

Se añadió:

```java
private ArchRule domainMustNotDependOnJpaOrHibernate() {
    return noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage(
                    "jakarta.persistence..",
                    "org.hibernate.."
            )
            .allowEmptyShould(true);
}
```

Resultado:

```text
Tests run: 2
Failures: 0
Errors: 0
BUILD SUCCESS
```

---

# 8. Slice 3 — Prohibir `domain → adapters`

Este slice es diferente: no prohíbe una tecnología externa, sino una **dirección interna de dependencias**.

Queremos permitir conceptualmente:

```text
adapter → domain      ✅
```

pero impedir:

```text
domain → adapter      ❌
```

## Paso 8.1 — Crear adapter ficticio

```java
package com.leovinci.leos.architecturefixtures.adapters;

public class FakeAdapter {
}
```

## Paso 8.2 — Crear dominio que depende del adapter

```java
package com.leovinci.leos.architecturefixtures.domain.adapter;

import com.leovinci.leos.architecturefixtures.adapters.FakeAdapter;

public class DomainDependingOnAdapter {

    private final FakeAdapter adapter;

    public DomainDependingOnAdapter(FakeAdapter adapter) {
        this.adapter = adapter;
    }
}
```

Relación controlada:

```text
..domain..DomainDependingOnAdapter
               ↓
..adapters..FakeAdapter
```

---

## Paso 8.3 — RED

Se añadió la expectativa:

```java
@Test
void adapterDomainFixtureIsRejected() {
    JavaClasses fixtureClasses =
            new ClassFileImporter().importClasses(DomainDependingOnAdapter.class);

    assertThrows(
            AssertionError.class,
            () -> domainMustNotDependOnAdapters().check(fixtureClasses)
    );
}
```

sin implementar aún la regla.

Resultado limpio:

```text
cannot find symbol
method domainMustNotDependOnAdapters()
BUILD FAILURE
```

---

## Paso 8.4 — GREEN

```java
private ArchRule domainMustNotDependOnAdapters() {
    return noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapters..")
            .allowEmptyShould(true);
}
```

Resultado:

```text
Tests run: 3
Failures: 0
Errors: 0
BUILD SUCCESS
```

---

# 9. Slice 4 — Prohibir `adapters.in.rest → adapters.out.persistence`

La intención arquitectónica es evitar que una entrada HTTP se salte los casos de uso o puertos de entrada y acceda directamente a una implementación concreta de persistencia.

Deseado:

```text
HTTP / adaptador REST de entrada
      ↓
Application / input port
      ↓
puertos de salida
      ↓
adaptador de persistencia
```

No deseado:

```text
adapters.in.rest
      ↓
adapters.out.persistence
```

La arquitectura objetivo de LC-IA expresa los adaptadores, cuando proceda, con una estructura del tipo:

```text
<capacidad>
└── adapters
    ├── in
    │   └── rest
    └── out
        └── persistence
```

Por tanto, esta cuarta regla debía proteger esa topología real y no una convención artificial basada en paquetes llamados literalmente `controller` o `adapters.persistence`.

## Paso 9.1 — Primera fixture simplificada

La primera versión de las fixtures usó paquetes simplificados:

```text
..controller..
..adapters.persistence..
```

La regla correspondiente consiguió un primer GREEN y demostró que ArchUnit podía detectar una dependencia directa entre ambos roles.

Sin embargo, esa prueba solo demostraba que **la regla coincidía con la fixture que nosotros mismos habíamos diseñado**. No demostraba que el selector cubriera la arquitectura real prevista para LC-IA.

## Paso 9.2 — Remodelar la fixture según la arquitectura objetivo

Se movió la fixture de persistencia a:

```text
src/test/java/com/leovinci/leos/architecturefixtures/document/adapters/out/persistence/FakePersistenceAdapter.java
```

```java
package com.leovinci.leos.architecturefixtures.document.adapters.out.persistence;

public class FakePersistenceAdapter {
}
```

Y la fixture de entrada REST a:

```text
src/test/java/com/leovinci/leos/architecturefixtures/document/adapters/in/rest/ControllerDependingOnPersistenceAdapter.java
```

```java
package com.leovinci.leos.architecturefixtures.document.adapters.in.rest;

import com.leovinci.leos.architecturefixtures.document.adapters.out.persistence.FakePersistenceAdapter;

public class ControllerDependingOnPersistenceAdapter {

    private final FakePersistenceAdapter persistenceAdapter;

    public ControllerDependingOnPersistenceAdapter(
            FakePersistenceAdapter persistenceAdapter
    ) {
        this.persistenceAdapter = persistenceAdapter;
    }
}
```

La violación controlada pasó a representar:

```text
document.adapters.in.rest.ControllerDependingOnPersistenceAdapter
                       ↓
document.adapters.out.persistence.FakePersistenceAdapter
```

## Paso 9.3 — RED semántico con la regla antigua

Se mantuvo deliberadamente la regla antigua:

```java
private ArchRule controllersMustNotDependOnPersistenceAdapters() {
    return noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapters.persistence..")
            .allowEmptyShould(true);
}
```

Y se ejecutó:

```powershell
mvn clean test -Dtest=ArchitectureBoundaryGuardsTest
```

Resultado:

```text
Tests run: 8, Failures: 1, Errors: 0

Expected java.lang.AssertionError to be thrown, but nothing was thrown.

BUILD FAILURE
```

Este RED fue especialmente valioso: la regla existía, compilaba y se ejecutaba, pero **dejaba escapar una violación compatible con la arquitectura objetivo**.

La causa era que ninguno de estos patrones:

```text
..controller..
..adapters.persistence..
```

coincidía con:

```text
..adapters.in.rest..
..adapters.out.persistence..
```

## Paso 9.4 — GREEN con selectores alineados con la arquitectura real

La regla se corrigió a:

```java
private ArchRule controllersMustNotDependOnPersistenceAdapters() {
    return noClasses()
            .that().resideInAPackage("..adapters.in.rest..")
            .should().dependOnClassesThat()
            .resideInAnyPackage("..adapters.out.persistence..")
            .allowEmptyShould(true);
}
```

Lectura:

> Ninguna clase del adaptador REST de entrada puede depender directamente de una clase del adaptador de persistencia de salida.

Resultado:

```text
Tests run: 8
Failures: 0
Errors: 0
BUILD SUCCESS
```

Este ciclo añadió una comprobación más fuerte que el RED inicial por método inexistente:

```text
fixture simplificada → GREEN
fixture arquitectónicamente realista → RED
selector corregido → GREEN
```

Por tanto, la cuarta guarda no solo demuestra que ArchUnit funciona: demuestra que el selector está alineado con la estructura de adaptadores que LC-IA pretende usar.

---

# 10. `allowEmptyShould(true)` y el problema de la producción todavía vacía

Las reglas seleccionan clases por su papel semántico en paquetes como:

```text
..domain..
..adapters.in.rest..
```

Hoy producción todavía no contiene clases de dominio ni adaptadores REST reales.

Sin una decisión explícita, una regla podría fallar únicamente porque no encuentra clases a las que aplicar el `should`.

Añadimos:

```java
.allowEmptyShould(true)
```

para expresar conscientemente:

> En este momento es válido que producción no contenga ninguna clase de ese rol.

Esto es distinto de desactivar globalmente la comprobación de conjuntos vacíos. La excepción queda localizada en cada regla de M0.4.

Las fixtures evitan que este permiso produzca una falsa sensación de seguridad:

```text
producción sin clases aplicables
        ↓
puede pasar vacíamente

fixture deliberadamente inválida
        ↓
debe ser rechazada
```

Así separamos dos preguntas:

1. ¿Existe hoy una violación en producción?
2. ¿La regla es capaz de detectar la violación cuando aparezca?

---

# 11. Conectar las reglas al código real de producción

Hasta este punto habíamos demostrado que cada detector podía detectar una violación conocida.

Pero eso todavía no protegía `src/main/java`.

La siguiente fase fue ejecutar exactamente las mismas reglas sobre las clases productivas compiladas.

---

## Paso 11.1 — Importar `target/classes`

En lugar de escribir una ruta fija:

```java
Path.of("target/classes")
```

se obtiene la ubicación real desde `LcIaApplication.class`:

```java
Path productionClassesPath = Path.of(
        LcIaApplication.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI()
);
```

Durante Maven esa clase procede de:

```text
lc-ia-server/target/classes/
```

Después ArchUnit importa esa ubicación:

```java
new ClassFileImporter().importPath(productionClassesPath);
```

### Por qué esta separación es importante

Las fixtures están en:

```text
target/test-classes/
```

mientras producción está en:

```text
target/classes/
```

Si accidentalmente importásemos las fixtures junto a producción, la regla `domain → Spring` detectaría `DomainDependingOnSpring` y el test productivo fallaría.

El GREEN obtenido constituye una comprobación indirecta de que estamos inspeccionando el conjunto correcto.

---

## Paso 11.2 — Primera prueba productiva

```java
@Test
void productionDomainDoesNotDependOnSpring()
        throws URISyntaxException {

    domainMustNotDependOnSpring()
            .check(productionClasses());
}
```

Al principio se implementó de forma explícita, sin helper, para validar primero el mecanismo de carga.

Resultado:

```text
Tests run: 5
Failures: 0
Errors: 0
BUILD SUCCESS
```

Interpretación:

```text
4 tests → demuestran los detectores mediante fixtures
1 test  → aplica una regla al código real
```

---

## Paso 11.3 — Aplicar las cuatro reglas a producción

Se añadieron pruebas productivas para:

```text
production domain ↛ Spring
production domain ↛ JPA/Hibernate
production domain ↛ adapters
production controller ↛ persistence adapter
```

Resultado:

```text
Tests run: 8
Failures: 0
Errors: 0
BUILD SUCCESS
```

La matriz resultante es:

| Restricción                                  | Prueba del detector | Prueba sobre producción |
| -------------------------------------------- | ------------------: | ----------------------: |
| `domain → Spring` prohibido                  |                  ✅ |                      ✅ |
| `domain → JPA/Hibernate` prohibido           |                  ✅ |                      ✅ |
| `domain → adapters` prohibido                |                  ✅ |                      ✅ |
| `controller → persistence adapter` prohibido |                  ✅ |                      ✅ |

---

# 12. Refactor después del GREEN

Los cuatro tests productivos repetían la lógica para localizar e importar `target/classes`.

Una vez demostrado el comportamiento se aplicó un refactor, sin cambiar semántica.

Helper:

```java
private JavaClasses productionClasses() throws URISyntaxException {
    Path productionClassesPath = Path.of(
            LcIaApplication.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
    );

    return new ClassFileImporter().importPath(productionClassesPath);
}
```

Los tests quedan expresivos:

```java
@Test
void productionDomainDoesNotDependOnAdapters()
        throws URISyntaxException {

    domainMustNotDependOnAdapters()
            .check(productionClasses());
}
```

### Por qué este refactor mejora el diseño

Separamos:

```text
CÓMO localizar producción
        ↓
productionClasses()
```

de:

```text
QUÉ restricción queremos verificar
        ↓
productionDomainDoesNotDependOnAdapters()
```

No se creó una abstracción genérica tipo `checkProductionRule(...)` porque ocultaría innecesariamente el lenguaje arquitectónico de cada test.

Tras el refactor:

```text
Tests run: 8
Failures: 0
Errors: 0
BUILD SUCCESS
```

Por tanto el ciclo quedó:

```text
RED → GREEN → REFACTOR
```

---

# 13. Estado actual del árbol de tests

Conceptualmente tenemos:

```text
src/test/java/com/leovinci/leos/
├── ArchitectureBoundaryGuardsTest.java
├── LcIaApplicationTest.java
└── architecturefixtures/
    ├── domain/
    │   ├── spring/
    │   │   └── DomainDependingOnSpring.java
    │   ├── jpa/
    │   │   └── DomainDependingOnJpa.java
    │   └── adapter/
    │       └── DomainDependingOnAdapter.java
    ├── adapters/
    │   └── FakeAdapter.java
    └── document/
        └── adapters/
            ├── in/
            │   └── rest/
            │       └── ControllerDependingOnPersistenceAdapter.java
            └── out/
                └── persistence/
                    └── FakePersistenceAdapter.java
```

Producción continúa sin clases arquitectónicas ficticias:

```text
src/main/java/com/leovinci/leos/
└── LcIaApplication.java
```

Ese aislamiento es deliberado: M0.4 instala guardas antes de que crezcan las capacidades reales, sin fabricar una arquitectura productiva vacía solo para que los tests tengan algo que inspeccionar.

---

# 14. Qué significan realmente los ocho tests

Es importante no interpretar los ocho verdes como ocho restricciones distintas.

Tenemos **cuatro restricciones**, cada una verificada desde dos ángulos.

## Nivel A — Prueba del detector

Ejemplo:

```text
fixture domain → Spring
        ↓
ArchUnit debe rechazarla
```

Responde a:

> ¿La alarma es capaz de detectar este tipo de incumplimiento?

## Nivel B — Evaluación productiva

Ejemplo:

```text
target/classes
        ↓
domainMustNotDependOnSpring()
```

Responde a:

> ¿Existe actualmente este incumplimiento en producción?

La combinación evita confundir:

```text
"no hay violaciones"
```

con:

```text
"el detector realmente funciona"
```

---

# 15. Resumen del TDD realizado

| Slice                                               | Fixture                                                              | RED observado                                        | GREEN observado                                  |
| --------------------------------------------------- | -------------------------------------------------------------------- | ---------------------------------------------------- | ------------------------------------------------ |
| `domain → Spring`                                   | `DomainDependingOnSpring`                                            | método de regla inexistente                          | fixture rechazada correctamente                  |
| `domain → JPA/Hibernate`                            | `DomainDependingOnJpa`                                               | método de regla inexistente                          | fixture JPA rechazada                            |
| `domain → adapters`                                 | `DomainDependingOnAdapter` + `FakeAdapter`                           | método de regla inexistente                          | dependencia interna rechazada                    |
| entrada REST → persistencia                         | `ControllerDependingOnPersistenceAdapter` + `FakePersistenceAdapter` | método de regla inexistente                          | salto directo rechazado con fixture simplificada |
| validación semántica del selector REST/persistencia | fixture realista en `adapters.in.rest` y `adapters.out.persistence`  | `Expected AssertionError ... but nothing was thrown` | selector corregido y fixture rechazada           |

Después:

```text
4 detectores GREEN
        ↓
4 checks de producción GREEN
        ↓
refactor de carga de producción
        ↓
8 tests ArchitectureBoundaryGuardsTest GREEN
        ↓
mvn clean test
        ↓
8 architecture guards + 1 Spring Boot smoke test
        ↓
9 tests GREEN
```

La última ejecución integrada observada fue:

```text
Tests run: 9
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

---

# 16. Qué NO hemos hecho en M0.4

Hasta este punto no se ha:

- creado dominio productivo ficticio;
- creado controllers productivos ficticios;
- añadido JPA al runtime;
- añadido Spring Data JPA;
- creado persistencia;
- cambiado comportamiento de LC-IA;
- añadido CI;
- tocado `synthetic-retrieval`;
- impuesto una jerarquía completa de paquetes a todas las futuras capacidades;
- introducido Maven dependency bans como mecanismo principal.

M0.4 sigue siendo una protección de límites arquitectónicos ejecutada como parte de tests.

La conexión a CI pertenece al hito posterior M0.5.

---

# 17. Warnings observados y por qué no forman parte de M0.4

Durante Maven aparecen warnings sobre:

- `System::load` utilizado por Jansi;
- `sun.misc.Unsafe` utilizado por Guava;
- Mockito auto-adjuntando su agente;
- carga dinámica de ByteBuddy en Java 25.

Estos warnings ya aparecían en el baseline y no están causados por las reglas ArchUnit.

No deben mezclarse con M0.4 salvo que se conviertan en un fallo real del build o se abra un cambio específico para modernizar esa configuración.

---

# 18. Validación semántica de selectores — resuelta

Durante la revisión previa al cierre se detectó un riesgo real: la cuarta regla inicial buscaba:

```text
..controller..
..adapters.persistence..
```

mientras la arquitectura objetivo de LC-IA contempla una estructura más expresiva:

```text
adapters.in.rest
adapters.out.persistence
```

No se dio por válida la equivalencia.

En lugar de modificar la regla por intuición, se realizó un nuevo ciclo TDD:

1. remodelar las fixtures para usar la topología objetivo;
2. mantener la regla antigua;
3. ejecutar el test;
4. observar un RED funcional porque la regla no detectaba la violación;
5. corregir únicamente los selectores;
6. volver a ejecutar y obtener GREEN.

Selectores finales:

```java
.that().resideInAPackage("..adapters.in.rest..")
.should().dependOnClassesThat()
.resideInAnyPackage("..adapters.out.persistence..")
```

Resultado:

```text
Tests run: 8
Failures: 0
Errors: 0
BUILD SUCCESS
```

Por tanto, el riesgo documentado inicialmente queda **resuelto dentro de M0.4**.

La protección final es ligeramente más general que «una clase cuyo nombre termina en Controller»: protege todo el adaptador REST de entrada frente a dependencias directas hacia el adaptador concreto de persistencia. Esto es coherente con la dirección de dependencias de la arquitectura hexagonal del proyecto.

---

# 19. Estado actual de M0.4

## Completado

- [x] baseline Maven verde;
- [x] ArchUnit 1.5.0 en test scope;
- [x] Jakarta Persistence API solo en test scope;
- [x] fixture `domain → Spring`;
- [x] RED y GREEN de `domain → Spring`;
- [x] fixture `domain → JPA`;
- [x] RED y GREEN de `domain → JPA/Hibernate`;
- [x] fixtures `domain → adapters`;
- [x] RED y GREEN de `domain → adapters`;
- [x] fixtures de entrada REST → persistencia;
- [x] RED y GREEN inicial de esa frontera;
- [x] remodelado de fixture según `adapters.in.rest` / `adapters.out.persistence`;
- [x] RED semántico que demostró que el selector antiguo era insuficiente;
- [x] GREEN con selectores alineados con la arquitectura objetivo;
- [x] comportamiento vacío permitido explícitamente mediante `allowEmptyShould(true)`;
- [x] importación separada de producción desde la ubicación real de `LcIaApplication.class`;
- [x] cuatro reglas aplicadas a `target/classes`;
- [x] ocho tests de arquitectura verdes;
- [x] refactor de la carga de clases productivas mediante `productionClasses()`;
- [x] suite Maven completa ejecutada con descubrimiento normal de Surefire;
- [x] suite completa: 9 tests, 0 failures, 0 errors, `BUILD SUCCESS`;
- [x] revisión del árbol de trabajo: ningún cambio en `src/main/java`;
- [x] eliminación de un archivo accidental llamado `-` generado por un Effective POM;
- [x] revisión de `pom.xml`: únicamente dependencias test-scoped de Jakarta Persistence y ArchUnit;
- [x] tamaño de implementación/configuración contenido: ~205 líneas, excluyendo la guía pedagógica.
- [x] reconciliación de `design.md` y `tasks.md` con la topología final;
- [x] verificación formal SDD con 4/4 requisitos y 10/10 escenarios;
- [x] `mvn package` ejecutado correctamente;
- [x] native attempt cerrado con resultado `passed`;
- [x] spec promovida a canónica;
- [x] change archivado;
- [x] commit de implementación creado como `865b7cf`.

## Estado final del repositorio

La implementación de M0.4 quedó versionada en el commit:

````text
865b7cf test(architecture): protect LC-IA module boundaries

---

# 20. Modelo mental final

M0.4 no está construyendo la arquitectura de LC-IA.

Está colocando **barandillas** alrededor de decisiones arquitectónicas que ya hemos tomado.

```text
                 LC-IA
                   │
        ┌──────────┴──────────┐
        │                     │
     dominio               adapters
        │                     │
        │  no puede conocer   │
        └─────────X──────────→ │

 dominio ─X→ Spring
 dominio ─X→ JPA/Hibernate

 controller
     │
     │ debe pasar por aplicación/puertos
     ↓
 application

 controller ─X→ persistence adapter concreto
````

Las fixtures prueban que las barandillas tienen resistencia.

Los checks sobre `target/classes` comprueban que el código real permanece dentro de ellas.

Y `allowEmptyShould(true)` nos permite instalar esas barandillas **antes** de que exista todo el edificio, sin tener que construir habitaciones falsas para demostrar que funcionan.

---

## Referencias internas del proyecto

Esta guía se ha elaborado en coherencia con las decisiones arquitectónicas actuales de LC-IA, especialmente:

- `AGENTS.md` — monolito modular hexagonal, dominio independiente de frameworks y adaptadores;
- `LC-IA — Inventario de modelos, clases, métodos, servicios y controladores` — estructura objetivo por capacidad y separación de adapters de entrada/salida;
- `LC-IA — Documento maestro de contexto, requisitos, casos de uso e implementación` — TDD estricto, arquitectura verificable y dominio desacoplado.
