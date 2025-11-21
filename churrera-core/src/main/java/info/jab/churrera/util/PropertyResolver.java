package info.jab.churrera.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Optional;

/**
 * Utility class for resolving properties from classpath resources.
 * Provides methods to read property values as strings.
 */
public class PropertyResolver {

    /**
     * Gets a property value as a string from a classpath resource.
     *
     * @param resourcePath the path to the properties file in classpath (e.g., "application.properties")
     * @param key the property key to retrieve
     * @return Optional containing the property value as string, or empty if not found
     */
    public Optional<String> getProperty(String resourcePath, String key) {
        // Precondition checks
        if (resourcePath == null || resourcePath.trim().isEmpty()) {
            return Optional.empty();
        }
        if (key == null || key.trim().isEmpty()) {
            return Optional.empty();
        }

        try {
            Properties properties = loadProperties(resourcePath);
            String value = properties.getProperty(key);

            if (value == null) {
                return Optional.empty();
            }

            return Optional.of(value);
        } catch (IOException _) {
            return Optional.empty();
        }
    }

    /**
     * Loads properties from a classpath resource.
     *
     * @param resourcePath the path to the properties file in classpath
     * @return Properties object containing all properties from the file
     * @throws IOException if the resource cannot be loaded
     */
    private Properties loadProperties(String resourcePath) throws IOException {
        // Precondition checks
        if (resourcePath == null) {
            throw new IOException("Resource path cannot be null");
        }
        if (resourcePath.trim().isEmpty()) {
            throw new IOException("Resource path cannot be empty");
        }

        Properties properties = new Properties();

        try (InputStream inputStream = PropertyResolver.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            properties.load(inputStream);
        }

        return properties;
    }
}
