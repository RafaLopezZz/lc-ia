package com.leovinci.leos;

import com.leovinci.leos.architecturefixtures.domain.spring.DomainDependingOnSpring;
import com.leovinci.leos.architecturefixtures.domain.jpa.DomainDependingOnJpa;
import com.leovinci.leos.architecturefixtures.document.adapters.in.rest.ControllerDependingOnPersistenceAdapter;
import com.leovinci.leos.architecturefixtures.domain.adapter.DomainDependingOnAdapter;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

public class ArchitectureBoundaryGuardsTest {

        @Test
        void springDomainFixtureIsRejected() {
                JavaClasses fixtureClasses = new ClassFileImporter().importClasses(DomainDependingOnSpring.class);

                assertThrows(
                                AssertionError.class,
                                () -> domainMustNotDependOnSpring().check(fixtureClasses));
        }

        private ArchRule domainMustNotDependOnSpring() {
                return noClasses()
                                .that().resideInAPackage("..domain..")
                                .should().dependOnClassesThat()
                                .resideInAnyPackage("org.springframework..")
                                .allowEmptyShould(true);
        }

        @Test
        void jpaOrHibernateDomainFixtureIsRejected() {
                JavaClasses fixtureClasses = new ClassFileImporter().importClasses(DomainDependingOnJpa.class);

                assertThrows(
                                AssertionError.class,
                                () -> domainMustNotDependOnJpaOrHibernate().check(fixtureClasses));
        }

        private ArchRule domainMustNotDependOnJpaOrHibernate() {
                return noClasses()
                                .that().resideInAPackage("..domain..")
                                .should().dependOnClassesThat()
                                .resideInAnyPackage(
                                                "jakarta.persistence..",
                                                "org.hibernate..")
                                .allowEmptyShould(true);
        }

        @Test
        void adapterDomainFixtureIsRejected() {
                JavaClasses fixtureClasses = new ClassFileImporter().importClasses(
                                DomainDependingOnAdapter.class);

                assertThrows(
                                AssertionError.class,
                                () -> domainMustNotDependOnAdapters().check(fixtureClasses));
        }

        private ArchRule domainMustNotDependOnAdapters() {
                return noClasses()
                                .that().resideInAPackage("..domain..")
                                .should().dependOnClassesThat()
                                .resideInAnyPackage("..adapters..")
                                .allowEmptyShould(true);
        }

        @Test
        void controllerDependingOnPersistenceAdapterIsRejected() {
                JavaClasses fixtureClasses = new ClassFileImporter().importClasses(
                                ControllerDependingOnPersistenceAdapter.class);

                assertThrows(
                                AssertionError.class,
                                () -> controllersMustNotDependOnPersistenceAdapters()
                                                .check(fixtureClasses));
        }

        private ArchRule controllersMustNotDependOnPersistenceAdapters() {
                return noClasses()
                                .that().resideInAPackage("..adapters.in.rest..")
                                .should().dependOnClassesThat()
                                .resideInAnyPackage("..adapters.out.persistence..")
                                .allowEmptyShould(true);
        }

        private JavaClasses productionClasses() throws URISyntaxException {
                Path productionClassesPath = Path.of(
                                LcIaApplication.class
                                                .getProtectionDomain()
                                                .getCodeSource()
                                                .getLocation()
                                                .toURI());

                return new ClassFileImporter().importPath(productionClassesPath);
        }

        @Test
        void productionDomainDoesNotDependOnSpring()
                        throws URISyntaxException {

                domainMustNotDependOnSpring()
                                .check(productionClasses());
        }

        @Test
        void productionDomainDoesNotDependOnJpaOrHibernate()
                        throws URISyntaxException {

                domainMustNotDependOnJpaOrHibernate()
                                .check(productionClasses());
        }

        @Test
        void productionDomainDoesNotDependOnAdapters()
                        throws URISyntaxException {

                domainMustNotDependOnAdapters()
                                .check(productionClasses());
        }

        @Test
        void productionControllersDoNotDependOnPersistenceAdapters()
                        throws URISyntaxException {

                controllersMustNotDependOnPersistenceAdapters()
                                .check(productionClasses());
        }

}
