package com.groovy.lsp.workspace;

import com.tngtech.archunit.junit5.AnalyzeClasses;
import com.tngtech.archunit.junit5.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.*;

/**
 * Architecture tests for the workspace-index module.
 * Ensures proper separation between API and internal packages.
 */
@AnalyzeClasses(packages = "com.groovy.lsp.workspace")
public class WorkspaceIndexArchitectureTest {
    
    private static final String API_PACKAGE = "com.groovy.lsp.workspace.api..";
    private static final String INTERNAL_PACKAGE = "com.groovy.lsp.workspace.internal..";
    private static final String DTO_PACKAGE = "com.groovy.lsp.workspace.api.dto..";
    
    @ArchTest
    static final ArchRule internal_packages_should_not_be_accessed_from_outside = 
        noClasses()
            .that().resideOutsideOfPackage(INTERNAL_PACKAGE)
            .should().accessClassesThat()
            .resideInAPackage(INTERNAL_PACKAGE)
            .because("Internal packages should not be accessed from outside");
    
    @ArchTest
    static final ArchRule api_should_only_contain_interfaces_and_dtos =
        classes()
            .that().resideInAPackage(API_PACKAGE)
            .and().doNotResideInAPackage(DTO_PACKAGE)
            .and().areNotInterfaces()
            .and().doNotHaveSimpleName("WorkspaceIndexFactory")
            .should().beInterfaces()
            .because("API package should only contain interfaces, DTOs, and factory");
    
    @ArchTest
    static final ArchRule dtos_should_be_records_or_enums =
        classes()
            .that().resideInAPackage(DTO_PACKAGE)
            .should().beRecords()
            .orShould().beEnums()
            .because("DTOs should be immutable records or enums");
    
    @ArchTest
    static final ArchRule index_and_dependency_should_be_internal =
        classes()
            .that().resideInAnyPackage("..index..", "..dependency..")
            .should().resideInAPackage(INTERNAL_PACKAGE)
            .because("Index and dependency resolution are internal implementation details");
    
    @ArchTest
    static final ArchRule implementation_classes_should_not_be_exposed =
        classes()
            .that().haveSimpleNameEndingWith("Impl")
            .or().haveSimpleName("SymbolIndex")
            .or().haveSimpleName("DependencyResolver")
            .should().notBePublic()
            .orShould().resideInAPackage(INTERNAL_PACKAGE)
            .because("Implementation classes should not be publicly accessible");
    
    @ArchTest
    static final ArchRule no_cycles_between_packages =
        slices()
            .matching("com.groovy.lsp.workspace.(*)..")
            .should().beFreeOfCycles()
            .because("Package structure should be acyclic");
    
    @ArchTest
    static final ArchRule layered_architecture = layeredArchitecture()
        .consideringAllDependencies()
        .layer("API").definedBy(API_PACKAGE)
        .layer("Internal").definedBy(INTERNAL_PACKAGE)
        .layer("External").definedBy("org.gradle..", "org.lmdbjava..")
        .whereLayer("API").mayNotAccessAnyLayer()
        .whereLayer("Internal").mayOnlyAccessLayers("API", "External")
        .because("Proper layering should be maintained");
}