package com.groovy.lsp.shared;

import com.tngtech.archunit.junit5.AnalyzeClasses;
import com.tngtech.archunit.junit5.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

/**
 * Architecture tests for jMolecules annotations and patterns.
 * Ensures that jMolecules annotations are used correctly and consistently.
 */
@AnalyzeClasses(packages = "com.groovy.lsp")
public class jMoleculesArchitectureTest {
    
    @ArchTest
    static final ArchRule domain_events_should_extend_base_class =
        classes()
            .that().areAnnotatedWith(org.jmolecules.event.annotation.DomainEvent.class)
            .should().beAssignableTo(com.groovy.lsp.shared.event.DomainEvent.class)
            .because("All domain events should extend the base DomainEvent class");
    
    @ArchTest
    static final ArchRule factories_should_have_static_factory_methods =
        classes()
            .that().areAnnotatedWith(org.jmolecules.ddd.annotation.Factory.class)
            .should().haveOnlyFinalFields()
            .orShould().haveNoFields()
            .because("Factories should be stateless or have only final fields");
    
    @ArchTest
    static final ArchRule services_should_be_interfaces =
        classes()
            .that().areAnnotatedWith(org.jmolecules.ddd.annotation.Service.class)
            .should().beInterfaces()
            .because("Domain services should be defined as interfaces");
    
    @ArchTest
    static final ArchRule value_objects_should_be_immutable =
        classes()
            .that().areAnnotatedWith(org.jmolecules.ddd.annotation.ValueObject.class)
            .should().beRecords()
            .orShould().beEnums()
            .orShould().haveOnlyFinalFields()
            .because("Value objects must be immutable");
    
    @ArchTest
    static final ArchRule modules_should_have_clear_boundaries =
        noClasses()
            .that().resideInAPackage("..internal..")
            .should().beAnnotatedWith(org.jmolecules.ddd.annotation.Module.class)
            .because("Module annotation should be on package level, not internal classes");
}