package ua.lpnu.kzp.direi.tomash;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads project version metadata generated from Maven.
 */
public final class VersionInfo {
    private static final String RESOURCE = "/version.properties";
    private static final Properties PROPERTIES = loadProperties();

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

    private static String required(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing version property: " + key);
        }
        return value;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream input = VersionInfo.class.getResourceAsStream(RESOURCE)) {
            if (input == null) {
                throw new IllegalStateException("Missing resource: " + RESOURCE);
            }
            properties.load(input);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read " + RESOURCE, exception);
        }
    }
}