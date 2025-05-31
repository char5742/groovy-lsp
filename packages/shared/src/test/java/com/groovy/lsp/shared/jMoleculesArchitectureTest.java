package com.groovy.lsp.shared;

import org.jmolecules.archunit.JMoleculesArchitectureRules;
import org.jmolecules.archunit.JMoleculesDddRules;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * jMolecules architecture tests.
 * Uses the standard jMolecules ArchUnit rules to validate DDD concepts and
 * architecture patterns.
 */
@AnalyzeClasses(packages = "com.groovy.lsp")
public class jMoleculesArchitectureTest {

    @ArchTest
    static final ArchRule dddRules = JMoleculesDddRules.all();

    @ArchTest
    static final ArchRule onionArchitecture = JMoleculesArchitectureRules.ensureOnionSimple();
}