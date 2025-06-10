package com.groovy.lsp.codenarc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.codenarc.rule.Rule;
import org.codenarc.rule.Violation;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * QuickFixMapperのテストクラス。
 */
@ExtendWith(MockitoExtension.class)
class QuickFixMapperTest {

    private QuickFixMapper quickFixMapper;

    @Mock private Violation mockViolation;

    @Mock private Rule mockRule;

    @BeforeEach
    void setUp() {
        quickFixMapper = new QuickFixMapper();
        lenient().when(mockViolation.getRule()).thenReturn(mockRule);
    }

    @Test
    void getSupportedRules_shouldReturnDefaultRegisteredRules() {
        // when
        Set<String> supportedRules = quickFixMapper.getSupportedRules();

        // then
        assertThat(supportedRules).isNotEmpty();
        assertThat(supportedRules)
                .contains(
                        "UnusedImport",
                        "DuplicateImport",
                        "ImportFromSamePackage",
                        "UnnecessaryGroovyImport",
                        "EmptyCatchBlock",
                        "EmptyElseBlock",
                        "EmptyFinallyBlock",
                        "EmptyIfStatement",
                        "EmptyWhileStatement",
                        "FieldName",
                        "MethodName",
                        "ClassName",
                        "VariableName",
                        "ExplicitArrayListInstantiation",
                        "ExplicitHashMapInstantiation",
                        "ExplicitLinkedListInstantiation",
                        "GStringAsMapKey",
                        "SimpleDateFormatMissingLocale",
                        "BooleanMethodReturnsNull");
    }

    @Test
    void getQuickFixesForViolation_shouldReturnQuickFixForUnusedImportRule() {
        // given
        when(mockRule.getName()).thenReturn("UnusedImport");
        lenient().when(mockViolation.getLineNumber()).thenReturn(5);

        // when
        List<CodeAction> actions = quickFixMapper.getQuickFixesForViolation(mockViolation);

        // then
        assertThat(actions).hasSize(1);
        CodeAction action = actions.get(0);
        assertThat(action.getTitle()).isEqualTo("Remove unused import");
        assertThat(action.getKind()).isEqualTo(CodeActionKind.QuickFix);
        assertThat(action.getEdit()).isNotNull();
    }

    @Test
    void getQuickFixesForViolation_shouldReturnQuickFixForDuplicateImportRule() {
        // given
        when(mockRule.getName()).thenReturn("DuplicateImport");
        lenient().when(mockViolation.getLineNumber()).thenReturn(10);

        // when
        List<CodeAction> actions =
                quickFixMapper.getQuickFixesForViolation(mockViolation, "file:///test.groovy");

        // then
        assertThat(actions).hasSize(1);
        CodeAction action = actions.get(0);
        assertThat(action.getTitle()).isEqualTo("Remove duplicate import");
        assertThat(action.getKind()).isEqualTo(CodeActionKind.QuickFix);
    }

    @Test
    void getQuickFixesForViolation_shouldReturnEmptyListForUnsupportedRule() {
        // given
        when(mockRule.getName()).thenReturn("UnsupportedRule");

        // when
        List<CodeAction> actions = quickFixMapper.getQuickFixesForViolation(mockViolation);

        // then
        assertThat(actions).isEmpty();
    }

    @Test
    void getQuickFixesForViolation_shouldReturnEmptyListWhenExceptionOccurs() {
        // given
        when(mockRule.getName()).thenReturn("UnusedImport");
        lenient()
                .when(mockViolation.getLineNumber())
                .thenThrow(new RuntimeException("Test exception"));

        // when
        List<CodeAction> actions = quickFixMapper.getQuickFixesForViolation(mockViolation);

        // then
        assertThat(actions).isEmpty();
    }

    @Test
    void registerProvider_shouldRegisterCustomProvider() {
        // given
        String customRule = "CustomRule";
        QuickFixMapper.QuickFixProvider provider =
                violation -> {
                    CodeAction action = new CodeAction();
                    action.setTitle("Custom fix");
                    return List.of(action);
                };

        // when
        quickFixMapper.registerProvider(customRule, provider);
        when(mockRule.getName()).thenReturn(customRule);

        // then
        List<CodeAction> actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Custom fix");
    }

    @Test
    void getQuickFixesForViolation_shouldReturnQuickFixForImportRelatedRules() {
        // Test ImportFromSamePackage
        when(mockRule.getName()).thenReturn("ImportFromSamePackage");
        lenient().when(mockViolation.getLineNumber()).thenReturn(3);
        List<CodeAction> actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Remove import from same package");

        // Test UnnecessaryGroovyImport
        when(mockRule.getName()).thenReturn("UnnecessaryGroovyImport");
        actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Remove unnecessary Groovy import");
    }

    @Test
    void getQuickFixesForViolation_shouldReturnQuickFixForBasicRules() {
        // Test EmptyCatchBlock
        when(mockRule.getName()).thenReturn("EmptyCatchBlock");
        lenient().when(mockViolation.getLineNumber()).thenReturn(15);
        List<CodeAction> actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Add log statement to catch block");

        // Test EmptyElseBlock
        when(mockRule.getName()).thenReturn("EmptyElseBlock");
        actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Remove empty else block");

        // Test EmptyFinallyBlock
        when(mockRule.getName()).thenReturn("EmptyFinallyBlock");
        actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Remove empty finally block");

        // Test EmptyIfStatement
        when(mockRule.getName()).thenReturn("EmptyIfStatement");
        actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Remove empty if statement");

        // Test EmptyWhileStatement
        when(mockRule.getName()).thenReturn("EmptyWhileStatement");
        actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Remove empty while statement");
    }

    @Test
    void getQuickFixesForViolation_shouldReturnQuickFixForNamingRules() {
        // Test FieldName
        when(mockRule.getName()).thenReturn("FieldName");
        lenient().when(mockViolation.getLineNumber()).thenReturn(20);
        List<CodeAction> actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Fix field name to follow conventions");

        // Test MethodName
        when(mockRule.getName()).thenReturn("MethodName");
        actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Fix method name to follow conventions");

        // Test ClassName
        when(mockRule.getName()).thenReturn("ClassName");
        actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Fix class name to follow conventions");

        // Test VariableName
        when(mockRule.getName()).thenReturn("VariableName");
        actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Fix variable name to follow conventions");
    }

    @Test
    void getQuickFixesForViolation_shouldReturnQuickFixForGroovyismRules() {
        // Test ExplicitArrayListInstantiation
        when(mockRule.getName()).thenReturn("ExplicitArrayListInstantiation");
        lenient().when(mockViolation.getLineNumber()).thenReturn(25);
        List<CodeAction> actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Use Groovy list literal []");

        // Test ExplicitHashMapInstantiation
        when(mockRule.getName()).thenReturn("ExplicitHashMapInstantiation");
        actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Use Groovy map literal [:]");

        // Test ExplicitLinkedListInstantiation
        when(mockRule.getName()).thenReturn("ExplicitLinkedListInstantiation");
        actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Use Groovy list literal [] as LinkedList");

        // Test GStringAsMapKey
        when(mockRule.getName()).thenReturn("GStringAsMapKey");
        actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Convert GString to String");
    }

    @Test
    void getQuickFixesForViolation_shouldReturnQuickFixForDesignRules() {
        // Test SimpleDateFormatMissingLocale
        when(mockRule.getName()).thenReturn("SimpleDateFormatMissingLocale");
        lenient().when(mockViolation.getLineNumber()).thenReturn(30);
        List<CodeAction> actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Add Locale parameter to SimpleDateFormat");

        // Test BooleanMethodReturnsNull
        when(mockRule.getName()).thenReturn("BooleanMethodReturnsNull");
        actions = quickFixMapper.getQuickFixesForViolation(mockViolation);
        assertThat(actions).hasSize(1);
        assertThat(actions.get(0).getTitle()).isEqualTo("Return false instead of null");
    }

    @Test
    void getQuickFixesForViolation_shouldUseProvidedFilePath() {
        // given
        String filePath = "file:///project/src/Test.groovy";
        when(mockRule.getName()).thenReturn("UnusedImport");
        lenient().when(mockViolation.getLineNumber()).thenReturn(5);

        // when
        List<CodeAction> actions =
                quickFixMapper.getQuickFixesForViolation(mockViolation, filePath);

        // then
        assertThat(actions).hasSize(1);
        CodeAction action = actions.get(0);
        assertThat(action.getEdit()).isNotNull();
        assertThat(action.getEdit().getChanges()).containsKey(filePath);
    }
}
