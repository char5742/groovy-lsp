package com.groovy.lsp.shared;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.groovy.lsp.test.annotations.E2ETest;
import com.groovy.lsp.test.annotations.IntegrationTest;
import com.groovy.lsp.test.annotations.PerformanceTest;
import com.groovy.lsp.test.annotations.SlowTest;
import com.groovy.lsp.test.annotations.UnitTest;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests to ensure proper usage of test annotations across the codebase.
 * Verifies that @Test is not used directly and that custom annotations are used instead.
 */
public class TestAnnotationUsageTest {

    private static final List<Class<? extends Annotation>> ALLOWED_TEST_ANNOTATIONS =
            Arrays.asList(
                    UnitTest.class,
                    IntegrationTest.class,
                    E2ETest.class,
                    PerformanceTest.class,
                    SlowTest.class);

    private static final String ALLOWED_ANNOTATIONS_LIST =
            String.join(
                    ", ",
                    Arrays.asList(
                            "@UnitTest",
                            "@IntegrationTest",
                            "@E2ETest",
                            "@PerformanceTest",
                            "@SlowTest"));

    @UnitTest
    void testNoDirectTestAnnotationUsage() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.groovy.lsp");

        noClasses()
                .that()
                .resideOutsideOfPackages(
                        "..test.annotations..", "..test.rules..", "com.groovy.lsp.shared")
                .should()
                .dependOnClassesThat()
                .areAssignableTo(Test.class)
                .because(
                        "Test methods should not use @Test directly. "
                                + "Use one of the custom test annotations instead: "
                                + ALLOWED_ANNOTATIONS_LIST)
                .check(classes);
    }

    @UnitTest
    void testAllTestMethodsUseCustomAnnotations() {
        JavaClasses classes = new ClassFileImporter().importPackages("com.groovy.lsp");

        methods()
                .that()
                .areDeclaredInClassesThat()
                .haveSimpleNameEndingWith("Test")
                .and()
                .arePublic()
                .and()
                .doNotHaveModifier(JavaModifier.STATIC)
                .and()
                .doNotHaveModifier(JavaModifier.ABSTRACT)
                .and()
                .haveNameMatching("test.*|.*Test")
                .and()
                .doNotHaveName("getSrcTest")
                .and()
                .doNotHaveName("addTestFile")
                .should(useOneOfCustomTestAnnotations())
                .allowEmptyShould(true)
                .because(
                        "Test methods should use one of the custom test annotations: "
                                + ALLOWED_ANNOTATIONS_LIST)
                .check(classes);
    }

    private static ArchCondition<JavaMethod> useOneOfCustomTestAnnotations() {
        return new ArchCondition<JavaMethod>("use one of the custom test annotations") {
            @Override
            public void check(JavaMethod method, ConditionEvents events) {
                // Skip methods in annotation classes themselves
                if (isInAnnotationClass(method)) {
                    return;
                }

                if (!hasAnyCustomTestAnnotation(method)) {
                    String message =
                            String.format(
                                    "Method %s.%s() does not use any of the custom test"
                                            + " annotations: %s",
                                    method.getOwner().getSimpleName(),
                                    method.getName(),
                                    ALLOWED_ANNOTATIONS_LIST);
                    events.add(SimpleConditionEvent.violated(method, message));
                }
            }
        };
    }

    private static boolean isInAnnotationClass(JavaMethod method) {
        String className = method.getOwner().getName();
        return className.contains("test.annotations");
    }

    private static boolean hasAnyCustomTestAnnotation(JavaMethod method) {
        for (Class<? extends Annotation> annotationClass : ALLOWED_TEST_ANNOTATIONS) {
            if (method.isAnnotatedWith(annotationClass)) {
                return true;
            }
        }
        return false;
    }
}
