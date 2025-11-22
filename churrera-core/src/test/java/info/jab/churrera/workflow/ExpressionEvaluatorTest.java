package info.jab.churrera.workflow;

import info.jab.churrera.util.PmlConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.ArrayList;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test class for ExpressionEvaluator.
 */
@DisplayName("ExpressionEvaluator Tests")
class ExpressionEvaluatorTest {

    private PmlConverter converter;

    @BeforeEach
    void setUp() {
        converter = new PmlConverter();
    }

    @Nested
    @DisplayName("Evaluate Tests")
    class EvaluateTests {

        @Test
        @DisplayName("Should evaluate get expression with integer")
        void shouldEvaluateGetExpressionWithInteger() {
            // When
            String result = ExpressionEvaluator.evaluate("$get()", 42);

            // Then
            assertThat(result).isEqualTo("42");
        }

        @Test
        @DisplayName("Should evaluate get expression with string")
        void shouldEvaluateGetExpressionWithString() {
            // When
            String result = ExpressionEvaluator.evaluate("$get()", "test-value");

            // Then
            assertThat(result).isEqualTo("test-value");
        }

        @Test
        @DisplayName("Should evaluate get expression with null")
        void shouldEvaluateGetExpressionWithNull() {
            // When
            String result = ExpressionEvaluator.evaluate("$get()", null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Should evaluate get expression with whitespace")
        void shouldEvaluateGetExpressionWithWhitespace() {
            // When
            String result = ExpressionEvaluator.evaluate("  $get()  ", 123);

            // Then
            assertThat(result).isEqualTo("123");
        }

        @ParameterizedTest
        @MethodSource("nullReturningExpressions")
        @DisplayName("Should return null for invalid or unsupported expressions")
        void shouldReturnNullForInvalidOrUnsupportedExpressions(String expression) {
            // When
            String result = ExpressionEvaluator.evaluate(expression, 42);

            // Then
            assertThat(result).isNull();
        }

        static Stream<String> nullReturningExpressions() {
            return Stream.of(null, "", "   ", "$other()");
        }
    }

    @Nested
    @DisplayName("ReplaceInputPlaceholder Tests")
    class ReplaceInputPlaceholderTests {

        @Test
        @DisplayName("Should replace input placeholder with value")
        void shouldReplaceInputPlaceholderWithValue() {
            // Given
            String content = "<?xml version=\"1.0\"?>\n<prompt>\n<goal>Process: <![CDATA[<input>INPUT</input>]]></goal>\n</prompt>";

            // When
            String result = ExpressionEvaluator.replaceInputPlaceholder(content, "5");

            // Then
            assertThat(result)
                .contains("<input>5</input>")
                .doesNotContain("<input>INPUT</input>");
        }

        @Test
        @DisplayName("Should replace multiple occurrences of input placeholder")
        void shouldReplaceMultipleOccurrencesOfInputPlaceholder() {
            // Given
            String content = "<input>INPUT</input> and also <input>INPUT</input>";

            // When
            String result = ExpressionEvaluator.replaceInputPlaceholder(content, "test");

            // Then
            assertThat(result).isEqualTo("<input>test</input> and also <input>test</input>");
        }

        @Test
        @DisplayName("Should not modify content without placeholder")
        void shouldNotModifyContentWithoutPlaceholder() {
            // Given
            String content = "<prompt>Some content without placeholder</prompt>";

            // When
            String result = ExpressionEvaluator.replaceInputPlaceholder(content, "value");

            // Then
            assertThat(result).isEqualTo(content);
        }

        @Test
        @DisplayName("Should return null for null content")
        void shouldReturnNullForNullContent() {
            // When
            String result = ExpressionEvaluator.replaceInputPlaceholder(null, "value");

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Should replace placeholder with null value")
        void shouldReplacePlaceholderWithNullValue() {
            // Given
            String content = "Process: <input>INPUT</input>";

            // When
            String result = ExpressionEvaluator.replaceInputPlaceholder(content, null);

            // Then
            assertThat(result).isEqualTo("Process: <input></input>");
        }

        @Test
        @DisplayName("Should replace placeholder with empty value")
        void shouldReplacePlaceholderWithEmptyValue() {
            // Given
            String content = "Process: <input>INPUT</input>";

            // When
            String result = ExpressionEvaluator.replaceInputPlaceholder(content, "");

            // Then
            assertThat(result).isEqualTo("Process: <input></input>");
        }

        @Test
        @DisplayName("Should replace placeholder with special characters")
        void shouldReplacePlaceholderWithSpecialCharacters() {
            // Given
            String content = "Process: <input>INPUT</input>";

            // When
            String result = ExpressionEvaluator.replaceInputPlaceholder(content, "<>&\"'");

            // Then
            assertThat(result).isEqualTo("Process: <input><>&\"'</input>");
        }
    }

    @Nested
    @DisplayName("IsSupported Tests")
    class IsSupportedTests {

        @Test
        @DisplayName("Should return true for get expression")
        void shouldReturnTrueForGetExpression() {
            // When & Then
            assertThat(ExpressionEvaluator.isSupported("$get()")).isTrue();
        }

        @Test
        @DisplayName("Should return true for get expression with whitespace")
        void shouldReturnTrueForGetExpressionWithWhitespace() {
            // When & Then
            assertThat(ExpressionEvaluator.isSupported("  $get()  ")).isTrue();
        }

        @Test
        @DisplayName("Should return false for null expression")
        void shouldReturnFalseForNullExpression() {
            // When & Then
            assertThat(ExpressionEvaluator.isSupported(null)).isFalse();
        }

        @Test
        @DisplayName("Should return false for empty expression")
        void shouldReturnFalseForEmptyExpression() {
            // When & Then
            assertThat(ExpressionEvaluator.isSupported("")).isFalse();
        }

        @Test
        @DisplayName("Should return false for whitespace expression")
        void shouldReturnFalseForWhitespaceExpression() {
            // When & Then
            assertThat(ExpressionEvaluator.isSupported("   ")).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {"$other()", "$GET()", "get()", "$get", "$(get)"})
        @DisplayName("Should return false for unsupported expressions")
        void shouldReturnFalseForUnsupportedExpressions(String expression) {
            // When & Then
            assertThat(ExpressionEvaluator.isSupported(expression)).isFalse();
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should evaluate expression and replace placeholder")
        void shouldEvaluateExpressionAndReplacePlaceholder() {
            // Given
            String expression = "$get()";
            Object value = 7;
            String promptContent = "<goal>Solve homework number: <![CDATA[<input>INPUT</input>]]></goal>";

            // When
            String evaluatedValue = ExpressionEvaluator.evaluate(expression, value);
            String modifiedContent = ExpressionEvaluator.replaceInputPlaceholder(promptContent, evaluatedValue);

            // Then
            assertThat(evaluatedValue).isNotNull().isEqualTo("7");
            assertThat(modifiedContent)
                .contains("<input>7</input>")
                .doesNotContain("<input>INPUT</input>");
        }

        @Test
        @DisplayName("Should process list of integers")
        void shouldProcessListOfIntegers() {
            // Given
            String expression = "$get()";
            String promptTemplate = "Process homework: <input>INPUT</input>";

            // When & Then
            for (int i = 1; i <= 4; i++) {
                String evaluated = ExpressionEvaluator.evaluate(expression, i);
                String modified = ExpressionEvaluator.replaceInputPlaceholder(promptTemplate, evaluated);

                assertThat(modified).isEqualTo("Process homework: <input>" + i + "</input>");
            }
        }

        @Test
        @DisplayName("Should handle CIS194 homework workflow example")
        void shouldHandleCis194HomeworkWorkflowExample() {
            // Given
            String promptContent = """
                ## Role

                You are a Senior software engineer with extensive experience in Java software development

                ## Goal

                Solve the Homework number: <input>INPUT</input>

                Get access to the page
                https://www.cis.upenn.edu/~cis1940/spring13/lectures.html

                to review the different Homeworks and check in the repository in
                sandbox/src/main/java/info/jab/cis194/

                a new package named homeworkX with a range from 1 to 12
                where X represent the week provided in the INPUT.
                """;

            // When
            String result1 = ExpressionEvaluator.replaceInputPlaceholder(promptContent, "1");
            String result2 = ExpressionEvaluator.replaceInputPlaceholder(promptContent, "2");
            String result3 = ExpressionEvaluator.replaceInputPlaceholder(promptContent, "3");
            String result4 = ExpressionEvaluator.replaceInputPlaceholder(promptContent, "4");

            // Then
            assertThat(result1)
                .contains("Solve the Homework number: <input>1</input>")
                .doesNotContain("<input>INPUT</input>");
            assertThat(result2)
                .contains("Solve the Homework number: <input>2</input>")
                .doesNotContain("<input>INPUT</input>");
            assertThat(result3)
                .contains("Solve the Homework number: <input>3</input>")
                .doesNotContain("<input>INPUT</input>");
            assertThat(result4)
                .contains("Solve the Homework number: <input>4</input>")
                .doesNotContain("<input>INPUT</input>");

            // Verify all results are different
            assertThat(result1)
                .isNotEqualTo(result2)
                .isNotEqualTo(result3)
                .isNotEqualTo(result4);
            assertThat(result2)
                .isNotEqualTo(result3)
                .isNotEqualTo(result4);
            assertThat(result3).isNotEqualTo(result4);
        }

        @Test
        @DisplayName("Should handle parallel job creation workflow")
        void shouldHandleParallelJobCreationWorkflow() {
            // Given
            String expression = "$get()";
            String promptTemplate = "Solve the Homework number: <input>INPUT</input>";
            List<Integer> resultList = List.of(1, 2, 3, 4);
            List<String> modifiedPrompts = new ArrayList<>();

            // When
            for (Integer element : resultList) {
                String evaluatedValue = ExpressionEvaluator.evaluate(expression, element);
                String modifiedContent = ExpressionEvaluator.replaceInputPlaceholder(promptTemplate, evaluatedValue);
                modifiedPrompts.add(modifiedContent);
            }

            // Then
            assertThat(modifiedPrompts)
                .hasSize(4)
                .containsExactly(
                    "Solve the Homework number: <input>1</input>",
                    "Solve the Homework number: <input>2</input>",
                    "Solve the Homework number: <input>3</input>",
                    "Solve the Homework number: <input>4</input>"
                );

            // Ensure no duplicates
            long distinctCount = modifiedPrompts.stream().distinct().count();
            assertThat(distinctCount).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("XML to Markdown Conversion Tests")
    class XmlToMarkdownConversionTests {

        @Test
        @DisplayName("Should preserve input pattern in markdown conversion")
        void shouldPreserveInputPatternInMarkdownConversion() {
            // Given
            String pmlXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <prompt xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:noNamespaceSchemaLocation="https://jabrena.github.io/pml/schemas/0.1.0/pml.xsd">
                    <role>You are a Senior software engineer</role>
                    <goal>Solve the Homework number: <![CDATA[<input>INPUT</input>]]>

                    Review the different Homeworks and check in the repository.
                    </goal>
                </prompt>
                """;

            // When
            String markdown = converter.toMarkdownFromContent(pmlXml);

            // Then
            assertThat(markdown)
                .isNotNull()
                .contains("<input>INPUT</input>")
                .contains("Solve the Homework number:");
        }

        @Test
        @DisplayName("Should apply replacement after markdown conversion")
        void shouldApplyReplacementAfterMarkdownConversion() {
            // Given
            String pmlXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <prompt xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:noNamespaceSchemaLocation="https://jabrena.github.io/pml/schemas/0.1.0/pml.xsd">
                    <role>You are a Senior software engineer</role>
                    <goal>Solve the Homework number: <![CDATA[<input>INPUT</input>]]>

                    where X represent the week provided in the INPUT.
                    </goal>
                </prompt>
                """;

            // When
            String markdown = converter.toMarkdownFromContent(pmlXml);
            String result = ExpressionEvaluator.replaceInputPlaceholder(markdown, "1");

            // Then
            assertThat(result)
                .isNotNull()
                .contains("<input>1</input>")
                .doesNotContain("<input>INPUT</input>")
                .contains("where X represent the week provided in the INPUT");
        }

        @Test
        @DisplayName("Should handle multiple values in markdown conversion")
        void shouldHandleMultipleValuesInMarkdownConversion() {
            // Given
            String pmlXml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <prompt xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                        xsi:noNamespaceSchemaLocation="https://jabrena.github.io/pml/schemas/0.1.0/pml.xsd">
                    <role>You are a Senior software engineer with extensive experience in Java software development</role>
                    <goal>Solve the Homework number: <![CDATA[<input>INPUT</input>]]>

                    Get access to the page
                    https://www.cis.upenn.edu/~cis1940/spring13/lectures.html

                    to review the different Homeworks and check in the repository in
                    sandbox/src/main/java/info/jab/cis194/

                    a new package named homeworkX with a range from 1 to 12
                    where X represent the week provided in the INPUT.
                    </goal>
                </prompt>
                """;

            // When
            String markdown = converter.toMarkdownFromContent(pmlXml);
            assertThat(markdown).contains("<input>INPUT</input>");

            String result1 = ExpressionEvaluator.replaceInputPlaceholder(markdown, "1");
            String result2 = ExpressionEvaluator.replaceInputPlaceholder(markdown, "2");
            String result3 = ExpressionEvaluator.replaceInputPlaceholder(markdown, "3");
            String result4 = ExpressionEvaluator.replaceInputPlaceholder(markdown, "4");

            // Then
            assertThat(result1).contains("<input>1</input>").doesNotContain("<input>INPUT</input>");
            assertThat(result2).contains("<input>2</input>").doesNotContain("<input>INPUT</input>");
            assertThat(result3).contains("<input>3</input>").doesNotContain("<input>INPUT</input>");
            assertThat(result4).contains("<input>4</input>").doesNotContain("<input>INPUT</input>");

            // Verify all results are unique
            assertThat(result1)
                .isNotEqualTo(result2)
                .isNotEqualTo(result3)
                .isNotEqualTo(result4);
            assertThat(result2).isNotEqualTo(result3);
        }
    }
}
