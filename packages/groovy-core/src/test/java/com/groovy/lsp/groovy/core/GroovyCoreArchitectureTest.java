package com.groovy.lsp.groovy.core;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.junit5.AnalyzeClasses;
import com.tngtech.archunit.junit5.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.*;

/**
 * Architecture tests for the groovy-core module.
 * Ensures proper separation between API and internal packages.
 */
@AnalyzeClasses(packages = "com.groovy.lsp.groovy.core")
public class GroovyCoreArchitectureTest {
    
    private static final String API_PACKAGE = "com.groovy.lsp.groovy.core.api..";
    private static final String INTERNAL_PACKAGE = "com.groovy.lsp.groovy.core.internal..";
    
    @ArchTest
    static final ArchRule internal_packages_should_not_be_accessed_from_outside = 
        noClasses()
            .that().resideOutsideOfPackage(INTERNAL_PACKAGE)
            .should().accessClassesThat()
            .resideInAPackage(INTERNAL_PACKAGE)
            .because("Internal packages should not be accessed from outside");
    
    @ArchTest
    static final ArchRule api_classes_should_be_interfaces_or_factories =
        classes()
            .that().resideInAPackage(API_PACKAGE)
            .and().areNotInterfaces()
            .and().doNotHaveSimpleName("GroovyCoreFactory")
            .and().areNotInnerClasses()
            .should().beInterfaces()
            .because("API package should only contain interfaces and factory classes");
    
    @ArchTest
    static final ArchRule implementation_classes_should_reside_in_internal_package =
        classes()
            .that().haveSimpleNameEndingWith("Impl")
            .should().resideInAPackage(INTERNAL_PACKAGE)
            .because("Implementation classes should reside in internal package");
    
    @ArchTest
    static final ArchRule services_should_be_accessed_through_interfaces =
        noClasses()
            .that().resideOutsideOfPackage("..internal.impl..")
            .should().directlyDependOnClassesThat()
            .haveSimpleNameEndingWith("Impl")
            .because("Services should be accessed through interfaces, not implementations");
    
    @ArchTest
    static final ArchRule factory_should_be_the_only_way_to_create_services =
        classes()
            .that().haveSimpleNameEndingWith("Impl")
            .should().onlyBeAccessed()
            .byClassesThat().resideInAnyPackage(
                "..internal.impl..",
                "..api.."
            )
            .because("Service implementations should only be created by factory");
    
    @ArchTest
    static final ArchRule layered_architecture = layeredArchitecture()
        .consideringAllDependencies()
        .layer("API").definedBy(API_PACKAGE)
        .layer("Internal").definedBy(INTERNAL_PACKAGE)
        .whereLayer("API").mayNotAccessAnyLayer()
        .whereLayer("Internal").mayOnlyAccessLayers("API")
        .because("API should not depend on internal implementation");
}