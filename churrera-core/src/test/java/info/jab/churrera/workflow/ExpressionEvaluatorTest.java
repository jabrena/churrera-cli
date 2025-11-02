package info.jab.churrera.workflow;

import info.jab.churrera.util.PmlConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;

import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ExpressionEvaluator.
 */
class ExpressionEvaluatorTest {

    private PmlConverter converter;

    @BeforeEach
    void setUp() {
        converter = new PmlConverter();
    }

    @Test
    void testEvaluate_GetExpression_WithInteger() {
        String result = ExpressionEvaluator.evaluate("$get()", 42);
        assertEquals("42", result);
    }

    @Test
    void testEvaluate_GetExpression_WithString() {
        String result = ExpressionEvaluator.evaluate("$get()", "test-value");
        assertEquals("test-value", result);
    }

    @Test
    void testEvaluate_GetExpression_WithNull() {
        String result = ExpressionEvaluator.evaluate("$get()", null);
        assertEquals("", result);
    }

    @Test
    void testEvaluate_GetExpression_WithWhitespace() {
        String result = ExpressionEvaluator.evaluate("  $get()  ", 123);
        assertEquals("123", result);
    }

    @Test
    void testEvaluate_NullExpression() {
        String result = ExpressionEvaluator.evaluate(null, 42);
        assertNull(result);
    }

    @Test
    void testEvaluate_EmptyExpression() {
        String result = ExpressionEvaluator.evaluate("", 42);
        assertNull(result);
    }

    @Test
    void testEvaluate_WhitespaceExpression() {
        String result = ExpressionEvaluator.evaluate("   ", 42);
        assertNull(result);
    }

    @Test
    void testEvaluate_UnsupportedExpression() {
        String result = ExpressionEvaluator.evaluate("$other()", 42);
        assertNull(result);
    }

    @Test
    void testReplaceInputPlaceholder_WithValue() {
        String content = "<?xml version=\"1.0\"?>\n<prompt>\n<goal>Process: <![CDATA[<input>INPUT</input>]]></goal>\n</prompt>";
        String result = ExpressionEvaluator.replaceInputPlaceholder(content, "5");

        assertTrue(result.contains("<input>5</input>"));
        assertFalse(result.contains("<input>INPUT</input>"));
    }

    @Test
    void testReplaceInputPlaceholder_MultipleOccurrences() {
        String content = "<input>INPUT</input> and also <input>INPUT</input>";
        String result = ExpressionEvaluator.replaceInputPlaceholder(content, "test");

        assertEquals("<input>test</input> and also <input>test</input>", result);
    }

    @Test
    void testReplaceInputPlaceholder_NoPlaceholder() {
        String content = "<prompt>Some content without placeholder</prompt>";
        String result = ExpressionEvaluator.replaceInputPlaceholder(content, "value");

        assertEquals(content, result);
    }

    @Test
    void testReplaceInputPlaceholder_NullContent() {
        String result = ExpressionEvaluator.replaceInputPlaceholder(null, "value");
        assertNull(result);
    }

    @Test
    void testReplaceInputPlaceholder_NullValue() {
        String content = "Process: <input>INPUT</input>";
        String result = ExpressionEvaluator.replaceInputPlaceholder(content, null);

        assertEquals("Process: <input></input>", result);
    }

    @Test
    void testReplaceInputPlaceholder_EmptyValue() {
        String content = "Process: <input>INPUT</input>";
        String result = ExpressionEvaluator.replaceInputPlaceholder(content, "");

        assertEquals("Process: <input></input>", result);
    }

    @Test
    void testReplaceInputPlaceholder_WithSpecialCharacters() {
        String content = "Process: <input>INPUT</input>";
        String result = ExpressionEvaluator.replaceInputPlaceholder(content, "<>&\"'");

        assertEquals("Process: <input><>&\"'</input>", result);
    }

    @Test
    void testIsSupported_GetExpression() {
        assertTrue(ExpressionEvaluator.isSupported("$get()"));
    }

    @Test
    void testIsSupported_GetExpression_WithWhitespace() {
        assertTrue(ExpressionEvaluator.isSupported("  $get()  "));
    }

    @Test
    void testIsSupported_NullExpression() {
        assertFalse(ExpressionEvaluator.isSupported(null));
    }

    @Test
    void testIsSupported_EmptyExpression() {
        assertFalse(ExpressionEvaluator.isSupported(""));
    }

    @Test
    void testIsSupported_WhitespaceExpression() {
        assertFalse(ExpressionEvaluator.isSupported("   "));
    }

    @ParameterizedTest
    @ValueSource(strings = {"$other()", "$GET()", "get()", "$get", "$(get)"})
    void testIsSupported_UnsupportedExpressions(String expression) {
        assertFalse(ExpressionEvaluator.isSupported(expression));
    }

    @Test
    void testIntegration_EvaluateAndReplace() {
        // Simulate the workflow: evaluate expression and replace placeholder
        String expression = "$get()";
        Object value = 7;
        String promptContent = "<goal>Solve homework number: <![CDATA[<input>INPUT</input>]]></goal>";

        // Evaluate expression
        String evaluatedValue = ExpressionEvaluator.evaluate(expression, value);
        assertNotNull(evaluatedValue);
        assertEquals("7", evaluatedValue);

        // Replace placeholder
        String modifiedContent = ExpressionEvaluator.replaceInputPlaceholder(promptContent, evaluatedValue);
        assertTrue(modifiedContent.contains("<input>7</input>"));
        assertFalse(modifiedContent.contains("<input>INPUT</input>"));
    }

    @Test
    void testIntegration_ListOfIntegers() {
        // Simulate processing a list of integers [1, 2, 3, 4]
        String expression = "$get()";
        String promptTemplate = "Process homework: <input>INPUT</input>";

        for (int i = 1; i <= 4; i++) {
            String evaluated = ExpressionEvaluator.evaluate(expression, i);
            String modified = ExpressionEvaluator.replaceInputPlaceholder(promptTemplate, evaluated);

            assertEquals("Process homework: <input>" + i + "</input>", modified);
        }
    }

    @Test
    void testRealWorldExample_CIS194Homework() {
        // This is the exact real-world example from the CIS194 homework workflow
        // that was failing - the INPUT placeholder was not being replaced
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

        // Test with different homework numbers to verify each gets unique content
        String result1 = ExpressionEvaluator.replaceInputPlaceholder(promptContent, "1");
        assertTrue(result1.contains("Solve the Homework number: <input>1</input>"));
        assertFalse(result1.contains("<input>INPUT</input>"));

        String result2 = ExpressionEvaluator.replaceInputPlaceholder(promptContent, "2");
        assertTrue(result2.contains("Solve the Homework number: <input>2</input>"));
        assertFalse(result2.contains("<input>INPUT</input>"));

        String result3 = ExpressionEvaluator.replaceInputPlaceholder(promptContent, "3");
        assertTrue(result3.contains("Solve the Homework number: <input>3</input>"));
        assertFalse(result3.contains("<input>INPUT</input>"));

        String result4 = ExpressionEvaluator.replaceInputPlaceholder(promptContent, "4");
        assertTrue(result4.contains("Solve the Homework number: <input>4</input>"));
        assertFalse(result4.contains("<input>INPUT</input>"));

        // Verify all results are different (this was the bug - they were all the same)
        assertNotEquals(result1, result2);
        assertNotEquals(result1, result3);
        assertNotEquals(result1, result4);
        assertNotEquals(result2, result3);
        assertNotEquals(result2, result4);
        assertNotEquals(result3, result4);
    }

    @Test
    void testCompleteWorkflow_ParallelJobCreation() {
        // Simulate the complete workflow for creating parallel jobs with bindResultExp
        String expression = "$get()";
        String promptTemplate = "Solve the Homework number: <input>INPUT</input>";

        // Parent job returns a list [1, 2, 3, 4]
        List<Integer> resultList = List.of(1, 2, 3, 4);
        List<String> modifiedPrompts = new ArrayList<>();

        // For each element, create a modified prompt
        for (Integer element : resultList) {
            // Step 1: Evaluate expression
            String evaluatedValue = ExpressionEvaluator.evaluate(expression, element);
            assertNotNull(evaluatedValue);

            // Step 2: Replace placeholder
            String modifiedContent = ExpressionEvaluator.replaceInputPlaceholder(promptTemplate, evaluatedValue);
            assertFalse(modifiedContent.contains("INPUT"));

            modifiedPrompts.add(modifiedContent);
        }

        // Verify each prompt is unique
        assertEquals(4, modifiedPrompts.size());
        assertEquals("Solve the Homework number: <input>1</input>", modifiedPrompts.get(0));
        assertEquals("Solve the Homework number: <input>2</input>", modifiedPrompts.get(1));
        assertEquals("Solve the Homework number: <input>3</input>", modifiedPrompts.get(2));
        assertEquals("Solve the Homework number: <input>4</input>", modifiedPrompts.get(3));

        // Ensure no duplicates
        long distinctCount = modifiedPrompts.stream().distinct().count();
        assertEquals(4, distinctCount);
    }

    @Test
    void testRealWorldExample_CIS194Homework_FullPrompt() {
        // This is the COMPLETE real-world example from the actual CIS194 homework workflow
        // with all the detailed instructions to ensure the replacement works end-to-end
        String fullPromptContent = """
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

            if in the repository doesn't exist a homeworkX, X represent the week, in that case take the minimum one which is not solved in the repository.
            In other case, continue with the next Homework if exist a previous one.
            If you are in Homework 9, jump to Homework 10 or higher.
            """;

        // Test with values 1, 2, 3, 4 (as returned by prompt-toc.xml)
        String result1 = ExpressionEvaluator.replaceInputPlaceholder(fullPromptContent, "1");
        String result2 = ExpressionEvaluator.replaceInputPlaceholder(fullPromptContent, "2");
        String result3 = ExpressionEvaluator.replaceInputPlaceholder(fullPromptContent, "3");
        String result4 = ExpressionEvaluator.replaceInputPlaceholder(fullPromptContent, "4");

        // Verify each result has the correct replaced value
        assertTrue(result1.contains("Solve the Homework number: <input>1</input>"));
        assertTrue(result2.contains("Solve the Homework number: <input>2</input>"));
        assertTrue(result3.contains("Solve the Homework number: <input>3</input>"));
        assertTrue(result4.contains("Solve the Homework number: <input>4</input>"));

        // Verify INPUT placeholder was completely removed
        assertFalse(result1.contains("<input>INPUT</input>"));
        assertFalse(result2.contains("<input>INPUT</input>"));
        assertFalse(result3.contains("<input>INPUT</input>"));
        assertFalse(result4.contains("<input>INPUT</input>"));

        // Verify the rest of the content is preserved
        assertTrue(result1.contains("where X represent the week provided in the INPUT"));
        assertTrue(result1.contains("If you are in Homework 9, jump to Homework 10 or higher"));

        // Verify all four results are unique
        assertNotEquals(result1, result2);
        assertNotEquals(result1, result3);
        assertNotEquals(result1, result4);
        assertNotEquals(result2, result3);
        assertNotEquals(result2, result4);
        assertNotEquals(result3, result4);

        // Verify the structure is maintained (contains "where X represent" text)
        long count1 = result1.chars().filter(ch -> ch == '<').count();
        long count2 = result2.chars().filter(ch -> ch == '<').count();
        assertEquals(count1, count2, "All results should have same structure");
    }

    @Test
    void testRealWorldExample_SimulateParallelExecution() {
        // Simulate the exact parallel execution flow:
        // 1. Parent job (prompt-toc.xml) returns [1, 2, 3, 4]
        // 2. Four child jobs are created, each with bindResultExp="$get()"
        // 3. Each child job processes prompt2.xml with its unique value

        String expression = "$get()";
        List<Integer> parentResult = List.of(1, 2, 3, 4);

        String originalPrompt = """
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

            if in the repository doesn't exist a homeworkX, X represent the week, in that case take the minimum one which is not solved in the repository.
            In other case, continue with the next Homework if exist a previous one.
            If you are in Homework 9, jump to Homework 10 or higher.
            """;

        Set<String> uniquePrompts = new HashSet<>();

        for (Integer value : parentResult) {
            // Step 1: Evaluate bindResultExp="$get()"
            String evaluatedValue = ExpressionEvaluator.evaluate(expression, value);
            assertNotNull(evaluatedValue);
            assertEquals(value.toString(), evaluatedValue);

            // Step 2: Read prompt2.xml (simulated)
            String promptContent = originalPrompt;

            // Step 3: Replace <input>INPUT</input> with the value
            String finalPrompt = ExpressionEvaluator.replaceInputPlaceholder(promptContent, evaluatedValue);

            // Step 4: Verify replacement worked
            assertTrue(finalPrompt.contains("Solve the Homework number: <input>" + value + "</input>"));
            assertFalse(finalPrompt.contains("<input>INPUT</input>"));

            // Collect unique prompts
            uniquePrompts.add(finalPrompt);
        }

        // Verify we got 4 unique prompts (not 4 copies of the same prompt)
        assertEquals(4, uniquePrompts.size(), "Each child job must receive a unique prompt");
    }

    @Test
    void testXmlToMarkdownConversion_PreservesInputPattern() {
        // Given: PML XML with CDATA containing <input>INPUT</input>
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

        // When: Convert XML to Markdown
        String markdown = converter.toMarkdownFromContent(pmlXml);

        // Then: Markdown should preserve the <input>INPUT</input> pattern
        assertNotNull(markdown);
        assertTrue(markdown.contains("<input>INPUT</input>"),
            "Markdown should contain the INPUT pattern");
        assertTrue(markdown.contains("Solve the Homework number:"),
            "Markdown should contain the goal content");
    }

    @Test
    void testXmlToMarkdownConversion_ThenReplacement() {
        // Given: PML XML with CDATA containing <input>INPUT</input>
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

        // When: Convert XML to Markdown
        String markdown = converter.toMarkdownFromContent(pmlXml);

        // Then: Apply replacement with value "1"
        String result = ExpressionEvaluator.replaceInputPlaceholder(markdown, "1");

        // Verify: Pattern was replaced
        assertNotNull(result);
        assertTrue(result.contains("<input>1</input>"),
            "Result should contain replaced value");
        assertFalse(result.contains("<input>INPUT</input>"),
            "Result should not contain original INPUT pattern");
        assertTrue(result.contains("where X represent the week provided in the INPUT"),
            "Other content should be preserved");
    }

    @Test
    void testXmlToMarkdownConversion_MultipleValues() {
        // Given: PML XML matching prompt2.xml structure
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

        // When: Convert XML to Markdown once
        String markdown = converter.toMarkdownFromContent(pmlXml);

        // Verify INPUT pattern is present
        assertTrue(markdown.contains("<input>INPUT</input>"));

        // Then: Apply replacement with different values
        String result1 = ExpressionEvaluator.replaceInputPlaceholder(markdown, "1");
        String result2 = ExpressionEvaluator.replaceInputPlaceholder(markdown, "2");
        String result3 = ExpressionEvaluator.replaceInputPlaceholder(markdown, "3");
        String result4 = ExpressionEvaluator.replaceInputPlaceholder(markdown, "4");

        // Verify: Each result has correct replacement
        assertTrue(result1.contains("<input>1</input>"));
        assertTrue(result2.contains("<input>2</input>"));
        assertTrue(result3.contains("<input>3</input>"));
        assertTrue(result4.contains("<input>4</input>"));

        // Verify: Original INPUT pattern is gone
        assertFalse(result1.contains("<input>INPUT</input>"));
        assertFalse(result2.contains("<input>INPUT</input>"));
        assertFalse(result3.contains("<input>INPUT</input>"));
        assertFalse(result4.contains("<input>INPUT</input>"));

        // Verify: All results are unique
        assertNotEquals(result1, result2);
        assertNotEquals(result1, result3);
        assertNotEquals(result1, result4);
        assertNotEquals(result2, result3);
    }
}

