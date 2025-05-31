package com.groovy.lsp.shared.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Architecture tests for the shared module.
 * These tests ensure that the module follows the established architectural principles.
 */
@AnalyzeClasses(
    packages = "com.groovy.lsp.shared",
    importOptions = ImportOption.DoNotIncludeTests.class
)
public class SharedModuleArchitectureTest {
    
    @ArchTest
    static final ArchRule internalPackagesShouldNotBeAccessedFromOutside = 
        noClasses()
            .that().resideOutsideOfPackage("com.groovy.lsp.shared.internal..")
            .should().accessClassesThat()
            .resideInAPackage("com.groovy.lsp.shared.internal..")
            .because("Internal packages should not be accessed from outside");
    
    @ArchTest
    static final ArchRule eventHandlersShouldOnlyDependOnEventPackage =
        classes()
            .that().implement("com.groovy.lsp.shared.event.EventHandler")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage(
                "com.groovy.lsp.shared.event..",
                "java..",
                "org.slf4j.."
            )
            .because("Event handlers should have minimal dependencies");
    
    @ArchTest
    static final ArchRule domainEventsShouldBeImmutable =
        classes()
            .that().areAssignableTo("com.groovy.lsp.shared.event.DomainEvent")
            .should().haveOnlyFinalFields()
            .because("Domain events should be immutable");
    
    @ArchTest
    static final ArchRule publicApiShouldBeAnnotated =
        classes()
            .that().resideInPackage("com.groovy.lsp.shared.event")
            .and().arePublic()
            .should().beAnnotatedWith("org.apiguardian.api.API")
            .because("Public API classes should be marked with @API annotation");
}