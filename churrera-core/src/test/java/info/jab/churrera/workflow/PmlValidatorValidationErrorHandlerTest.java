package info.jab.churrera.workflow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.xml.sax.SAXParseException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PmlValidator.ValidationErrorHandler.
 * Uses reflection to test the private inner class.
 */
@ExtendWith(MockitoExtension.class)
class PmlValidatorValidationErrorHandlerTest {

    private Object errorHandler;
    private Class<?> errorHandlerClass;
    private Method warningMethod;
    private Method errorMethod;
    private Method fatalErrorMethod;
    private Method hasErrorsMethod;
    private Method getErrorsMethod;

    @BeforeEach
    void setUp() throws Exception {
        // Get the ValidationErrorHandler class using reflection
        Class<?> validatorClass = PmlValidator.class;
        Class<?>[] innerClasses = validatorClass.getDeclaredClasses();
        
        for (Class<?> innerClass : innerClasses) {
            if (innerClass.getSimpleName().equals("ValidationErrorHandler")) {
                errorHandlerClass = innerClass;
                break;
            }
        }
        
        assertNotNull(errorHandlerClass, "ValidationErrorHandler class not found");
        
        // Get constructor and create instance
        Constructor<?> constructor = errorHandlerClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        errorHandler = constructor.newInstance();
        
        // Get methods
        warningMethod = errorHandlerClass.getMethod("warning", SAXParseException.class);
        errorMethod = errorHandlerClass.getMethod("error", SAXParseException.class);
        fatalErrorMethod = errorHandlerClass.getMethod("fatalError", SAXParseException.class);
        hasErrorsMethod = errorHandlerClass.getMethod("hasErrors");
        getErrorsMethod = errorHandlerClass.getMethod("getErrors");
    }

    @Test
    void testHasErrors_NoErrors() throws Exception {
        // When
        Boolean result = (Boolean) hasErrorsMethod.invoke(errorHandler);

        // Then
        assertFalse(result);
    }

    @Test
    void testGetErrors_NoErrors() throws Exception {
        // When
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void testWarning_AddsWarning() throws Exception {
        // Given
        SAXParseException exception = mock(SAXParseException.class);
        when(exception.getMessage()).thenReturn("Test warning message");
        when(exception.getLineNumber()).thenReturn(10);

        // When
        warningMethod.invoke(errorHandler, exception);
        Boolean hasErrors = (Boolean) hasErrorsMethod.invoke(errorHandler);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertTrue(hasErrors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Warning:"));
        assertTrue(errors.get(0).contains("Test warning message"));
        assertTrue(errors.get(0).contains("at line 10"));
    }

    @Test
    void testError_AddsError() throws Exception {
        // Given
        SAXParseException exception = mock(SAXParseException.class);
        when(exception.getMessage()).thenReturn("Test error message");
        when(exception.getLineNumber()).thenReturn(25);

        // When
        errorMethod.invoke(errorHandler, exception);
        Boolean hasErrors = (Boolean) hasErrorsMethod.invoke(errorHandler);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertTrue(hasErrors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Error:"));
        assertTrue(errors.get(0).contains("Test error message"));
        assertTrue(errors.get(0).contains("at line 25"));
    }

    @Test
    void testFatalError_AddsFatalError() throws Exception {
        // Given
        SAXParseException exception = mock(SAXParseException.class);
        when(exception.getMessage()).thenReturn("Test fatal error message");
        when(exception.getLineNumber()).thenReturn(50);

        // When
        fatalErrorMethod.invoke(errorHandler, exception);
        Boolean hasErrors = (Boolean) hasErrorsMethod.invoke(errorHandler);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertTrue(hasErrors);
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Fatal Error:"));
        assertTrue(errors.get(0).contains("Test fatal error message"));
        assertTrue(errors.get(0).contains("at line 50"));
    }

    @Test
    void testMultipleWarnings() throws Exception {
        // Given
        SAXParseException exception1 = mock(SAXParseException.class);
        when(exception1.getMessage()).thenReturn("Warning 1");
        when(exception1.getLineNumber()).thenReturn(5);

        SAXParseException exception2 = mock(SAXParseException.class);
        when(exception2.getMessage()).thenReturn("Warning 2");
        when(exception2.getLineNumber()).thenReturn(10);

        // When
        warningMethod.invoke(errorHandler, exception1);
        warningMethod.invoke(errorHandler, exception2);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).contains("Warning 1"));
        assertTrue(errors.get(0).contains("at line 5"));
        assertTrue(errors.get(1).contains("Warning 2"));
        assertTrue(errors.get(1).contains("at line 10"));
    }

    @Test
    void testMultipleErrors() throws Exception {
        // Given
        SAXParseException exception1 = mock(SAXParseException.class);
        when(exception1.getMessage()).thenReturn("Error 1");
        when(exception1.getLineNumber()).thenReturn(15);

        SAXParseException exception2 = mock(SAXParseException.class);
        when(exception2.getMessage()).thenReturn("Error 2");
        when(exception2.getLineNumber()).thenReturn(20);

        // When
        errorMethod.invoke(errorHandler, exception1);
        errorMethod.invoke(errorHandler, exception2);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertEquals(2, errors.size());
        assertTrue(errors.get(0).contains("Error:"));
        assertTrue(errors.get(0).contains("Error 1"));
        assertTrue(errors.get(0).contains("at line 15"));
        assertTrue(errors.get(1).contains("Error:"));
        assertTrue(errors.get(1).contains("Error 2"));
        assertTrue(errors.get(1).contains("at line 20"));
    }

    @Test
    void testMixedErrorTypes() throws Exception {
        // Given
        SAXParseException warning = mock(SAXParseException.class);
        when(warning.getMessage()).thenReturn("Warning message");
        when(warning.getLineNumber()).thenReturn(1);

        SAXParseException error = mock(SAXParseException.class);
        when(error.getMessage()).thenReturn("Error message");
        when(error.getLineNumber()).thenReturn(2);

        SAXParseException fatalError = mock(SAXParseException.class);
        when(fatalError.getMessage()).thenReturn("Fatal error message");
        when(fatalError.getLineNumber()).thenReturn(3);

        // When
        warningMethod.invoke(errorHandler, warning);
        errorMethod.invoke(errorHandler, error);
        fatalErrorMethod.invoke(errorHandler, fatalError);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertEquals(3, errors.size());
        assertTrue(errors.get(0).contains("Warning:"));
        assertTrue(errors.get(1).contains("Error:"));
        assertTrue(errors.get(2).contains("Fatal Error:"));
    }

    @Test
    void testGetErrors_ReturnsDefensiveCopy() throws Exception {
        // Given
        SAXParseException exception = mock(SAXParseException.class);
        when(exception.getMessage()).thenReturn("Test message");
        when(exception.getLineNumber()).thenReturn(1);
        errorMethod.invoke(errorHandler, exception);

        // When
        @SuppressWarnings("unchecked")
        List<String> errors1 = (List<String>) getErrorsMethod.invoke(errorHandler);
        errors1.add("Should not appear");
        @SuppressWarnings("unchecked")
        List<String> errors2 = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertEquals(1, errors2.size());
        assertFalse(errors2.contains("Should not appear"));
    }

    @Test
    void testErrorWithNullMessage() throws Exception {
        // Given
        SAXParseException exception = mock(SAXParseException.class);
        when(exception.getMessage()).thenReturn(null);
        when(exception.getLineNumber()).thenReturn(5);

        // When
        errorMethod.invoke(errorHandler, exception);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Error:"));
        assertTrue(errors.get(0).contains("at line 5"));
    }

    @Test
    void testErrorWithNegativeLineNumber() throws Exception {
        // Given
        SAXParseException exception = mock(SAXParseException.class);
        when(exception.getMessage()).thenReturn("Test message");
        when(exception.getLineNumber()).thenReturn(-1);

        // When
        errorMethod.invoke(errorHandler, exception);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Error:"));
        assertTrue(errors.get(0).contains("at line -1"));
    }

    @Test
    void testErrorWithZeroLineNumber() throws Exception {
        // Given
        SAXParseException exception = mock(SAXParseException.class);
        when(exception.getMessage()).thenReturn("Test message");
        when(exception.getLineNumber()).thenReturn(0);

        // When
        errorMethod.invoke(errorHandler, exception);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("Error:"));
        assertTrue(errors.get(0).contains("at line 0"));
    }

    @Test
    void testWarningErrorFatalError_AllTypes() throws Exception {
        // Given
        SAXParseException warning = mock(SAXParseException.class);
        when(warning.getMessage()).thenReturn("Warning");
        when(warning.getLineNumber()).thenReturn(1);

        SAXParseException error = mock(SAXParseException.class);
        when(error.getMessage()).thenReturn("Error");
        when(error.getLineNumber()).thenReturn(2);

        SAXParseException fatalError = mock(SAXParseException.class);
        when(fatalError.getMessage()).thenReturn("Fatal");
        when(fatalError.getLineNumber()).thenReturn(3);

        // When
        warningMethod.invoke(errorHandler, warning);
        errorMethod.invoke(errorHandler, error);
        fatalErrorMethod.invoke(errorHandler, fatalError);
        Boolean hasErrors = (Boolean) hasErrorsMethod.invoke(errorHandler);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertTrue(hasErrors);
        assertEquals(3, errors.size());
        assertTrue(errors.get(0).startsWith("Warning:"));
        assertTrue(errors.get(1).startsWith("Error:"));
        assertTrue(errors.get(2).startsWith("Fatal Error:"));
    }

    @Test
    void testErrorFormat_ContainsMessageAndLineNumber() throws Exception {
        // Given
        SAXParseException exception = mock(SAXParseException.class);
        when(exception.getMessage()).thenReturn("Custom error message");
        when(exception.getLineNumber()).thenReturn(42);

        // When
        errorMethod.invoke(errorHandler, exception);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertEquals(1, errors.size());
        String errorMessage = errors.get(0);
        assertEquals("Error: Custom error message at line 42", errorMessage);
    }

    @Test
    void testWarningFormat_ContainsMessageAndLineNumber() throws Exception {
        // Given
        SAXParseException exception = mock(SAXParseException.class);
        when(exception.getMessage()).thenReturn("Custom warning message");
        when(exception.getLineNumber()).thenReturn(100);

        // When
        warningMethod.invoke(errorHandler, exception);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertEquals(1, errors.size());
        String errorMessage = errors.get(0);
        assertEquals("Warning: Custom warning message at line 100", errorMessage);
    }

    @Test
    void testFatalErrorFormat_ContainsMessageAndLineNumber() throws Exception {
        // Given
        SAXParseException exception = mock(SAXParseException.class);
        when(exception.getMessage()).thenReturn("Custom fatal error message");
        when(exception.getLineNumber()).thenReturn(200);

        // When
        fatalErrorMethod.invoke(errorHandler, exception);
        @SuppressWarnings("unchecked")
        List<String> errors = (List<String>) getErrorsMethod.invoke(errorHandler);

        // Then
        assertEquals(1, errors.size());
        String errorMessage = errors.get(0);
        assertEquals("Fatal Error: Custom fatal error message at line 200", errorMessage);
    }
}

