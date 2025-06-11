package com.groovy.lsp.test.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Annotation for end-to-end tests. These tests verify the complete system behavior from the user's
 * perspective. They are the slowest tests and form the top of the test pyramid.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Test
@Tag("e2e")
public @interface E2ETest {}
