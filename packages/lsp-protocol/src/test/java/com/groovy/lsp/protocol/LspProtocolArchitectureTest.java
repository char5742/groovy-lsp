package com.groovy.lsp.protocol;

import com.tngtech.archunit.junit5.AnalyzeClasses;
import com.tngtech.archunit.junit5.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.*;

/**
 * Architecture tests for the lsp-protocol module.
 * Ensures proper separation between API and internal packages.
 */
@AnalyzeClasses(packages = "com.groovy.lsp.protocol")
public class LspProtocolArchitectureTest {
    
    private static final String API_PACKAGE = "com.groovy.lsp.protocol.api..";
    private static final String INTERNAL_PACKAGE = "com.groovy.lsp.protocol.internal..";
    
    @ArchTest
    static final ArchRule internal_packages_should_not_be_accessed_from_outside = 
        noClasses()
            .that().resideOutsideOfPackage(INTERNAL_PACKAGE)
            .should().accessClassesThat()
            .resideInAPackage(INTERNAL_PACKAGE)
            .because("Internal packages should not be accessed from outside");
    
    @ArchTest
    static final ArchRule language_server_should_be_in_api_package =
        classes()
            .that().haveSimpleNameContaining("LanguageServer")
            .and().areTopLevelClasses()
            .should().resideInAPackage(API_PACKAGE)
            .because("LanguageServer is the main API entry point");
    
    @ArchTest
    static final ArchRule service_implementations_should_be_internal =
        classes()
            .that().implement("org.eclipse.lsp4j.services.TextDocumentService")
            .or().implement("org.eclipse.lsp4j.services.WorkspaceService")
            .and().doNotHaveSimpleName("GroovyLanguageServer")
            .should().resideInAPackage(INTERNAL_PACKAGE)
            .because("Service implementations should be internal");
    
    @ArchTest
    static final ArchRule internal_services_should_be_package_private =
        classes()
            .that().resideInAPackage(INTERNAL_PACKAGE)
            .and().haveSimpleNameEndingWith("Service")
            .should().bePackagePrivate()
            .orShould().bePrivate()
            .because("Internal services should not be publicly accessible");
    
    @ArchTest
    static final ArchRule layered_architecture = layeredArchitecture()
        .consideringAllDependencies()
        .layer("API").definedBy(API_PACKAGE)
        .layer("Internal").definedBy(INTERNAL_PACKAGE)
        .layer("LSP4J").definedBy("org.eclipse.lsp4j..")
        .whereLayer("API").mayOnlyAccessLayers("LSP4J", "Internal")
        .whereLayer("Internal").mayOnlyAccessLayers("LSP4J")
        .because("Proper layering should be maintained");
}