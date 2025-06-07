package com.groovy.lsp.workspace.api.dto;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Types of symbols that can be indexed in a Groovy workspace.
 * This enumeration represents the different kinds of language constructs
 * that can be searched and navigated.
 *
 * Marked as a ValueObject as it's an immutable part of the symbol's characteristics.
 */
@ValueObject
public enum SymbolKind {
    /**
     * A class definition.
     */
    CLASS,

    /**
     * An interface definition.
     */
    INTERFACE,

    /**
     * A Groovy trait definition.
     */
    TRAIT,

    /**
     * A method or function definition.
     */
    METHOD,

    /**
     * A field definition.
     */
    FIELD,

    /**
     * A Groovy property definition.
     */
    PROPERTY,

    /**
     * A constructor definition.
     */
    CONSTRUCTOR,

    /**
     * An enum definition.
     */
    ENUM,

    /**
     * An enum constant definition.
     */
    ENUM_CONSTANT,

    /**
     * An annotation type definition.
     */
    ANNOTATION,

    /**
     * A closure expression in Groovy.
     */
    CLOSURE
}
