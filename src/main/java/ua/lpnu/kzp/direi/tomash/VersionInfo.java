package ua.lpnu.kzp.direi.tomash;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Provides the Maven project version embedded in the application resources.
 *
 * <p>The Maven build filters {@code version.properties} and stores the project
 * version there. Reading this single value keeps the CLI version aligned with
 * the version declared in {@code pom.xml}.</p>
 */
public final class VersionInfo {
    /** Classpath location of the Maven-filtered version properties. */
    private static final String RESOURCE = "/version.properties";

    /** Properties loaded once when this utility class is initialized. */
    private static final Properties PROPERTIES = loadProperties();

    /** Prevents instantiation of this utility class. */
    private VersionInfo() {
    }

    /**
     * Returns the Maven version of the current release.
     *
     * @return current project version
     */
    public static String projectVersion() {
        return required("project.version");
    }

    /**
     * Returns a required non-blank property from the loaded version metadata.
     *
     * @param key name of the property to retrieve
     * @return the property's non-blank value
     * @throws IllegalStateException if the property is absent or blank
     */
    private static String required(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing version property: %s".formatted(key));
        }
        return value;
    }

    /**
     * Loads the version properties from the classpath resource.
     *
     * @return loaded project metadata
     * @throws IllegalStateException if the resource is missing or cannot be read
     */
    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = VersionInfo.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: %s".formatted(RESOURCE));
            }
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read %s".formatted(RESOURCE), exception);
        }
    }
}