# Architecture Boundary Guards Specification

## Purpose

Define executable M0.4 dependency boundaries for compiled classes in the real `lc-ia-server` module, with test-only proof that each boundary detects a violation.

## Requirements

### Requirement: Maven-Executed Production Guard Evaluation

The system MUST execute compiled-class architecture guards as part of `lc-ia-server` `mvn test`. Each production evaluation MUST analyze only compiled classes originating in that module's `src/main/java`; test fixtures MUST NOT affect its result. A production guard MAY pass when no applicable production classes exist.

#### Scenario: Production guards run in the Maven test lifecycle

- GIVEN the `lc-ia-server` module is checked out
- WHEN `mvn test` is run in that module
- THEN all production architecture guards are evaluated
- AND the test result fails if an applicable production dependency violates a guard

#### Scenario: Sparse production code has no applicable class

- GIVEN an architecture category has no matching compiled production class
- WHEN its production guard is evaluated
- THEN the guard MAY pass without creating production placeholder classes

### Requirement: Domain Dependency Isolation

The system MUST ensure that applicable real production domain classes do not depend on Spring, JPA/Hibernate, or adapter code. The system MUST NOT introduce fake production packages or classes, prescribe a future package hierarchy, or change `synthetic-retrieval` to satisfy this requirement.

#### Scenario: Domain class has no forbidden dependency

- GIVEN a compiled production domain class without Spring, JPA/Hibernate, or adapter dependencies
- WHEN the domain isolation guard is evaluated
- THEN the guard passes

#### Scenario: Controlled Spring domain violation is rejected

- GIVEN a controlled compiled test case representing a domain class dependent on Spring
- WHEN the identical domain isolation guard is evaluated against that fixture
- THEN the evaluation rejects the fixture

#### Scenario: Controlled JPA/Hibernate domain violation is rejected

- GIVEN a controlled compiled test case representing a domain class dependent on JPA/Hibernate
- WHEN the identical domain isolation guard is evaluated against that fixture
- THEN the evaluation rejects the fixture

#### Scenario: Controlled adapter-code domain violation is rejected

- GIVEN a controlled compiled test case representing a domain class dependent on adapter code
- WHEN the identical domain isolation guard is evaluated against that fixture
- THEN the evaluation rejects the fixture

### Requirement: Controller Persistence-Adapter Isolation

The system MUST ensure that applicable real production controllers do not directly depend on concrete persistence adapters. The system MUST NOT require controllers, adapters, or a package hierarchy to exist before those production concepts are introduced.

#### Scenario: Controller has no direct concrete adapter dependency

- GIVEN a compiled production controller without a direct dependency on a concrete persistence adapter
- WHEN the controller isolation guard is evaluated
- THEN the guard passes

#### Scenario: Controlled controller-to-concrete-adapter violation is rejected

- GIVEN a controlled compiled test case representing a controller directly dependent on a concrete persistence adapter
- WHEN the identical controller isolation guard is evaluated against that fixture
- THEN the evaluation rejects the fixture

### Requirement: Independent Violation Proof and CI Readiness

The system MUST independently demonstrate that every forbidden dependency category is rejected, independently of the production evaluation. The guards MUST remain runnable by `mvn test` for later CI use. This change MUST NOT create or modify a CI workflow; CI wiring is M0.5 scope.

#### Scenario: Every forbidden dependency category has independent proof

- GIVEN all architecture guards and their forbidden dependency categories are present
- WHEN the test suite runs
- THEN Spring, JPA/Hibernate, adapter code, and controller-to-concrete-persistence-adapter dependencies are each independently demonstrated as rejected
- AND every rejection is asserted independently from the production result

#### Scenario: No M0.5 workflow change is needed

- GIVEN the M0.4 guard implementation is complete
- WHEN repository workflow files are inspected
- THEN no CI workflow creation or modification is required
- AND `mvn test` remains the CI-ready execution entry point
