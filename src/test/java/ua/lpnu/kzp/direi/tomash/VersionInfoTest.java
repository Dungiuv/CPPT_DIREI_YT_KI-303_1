package ua.lpnu.kzp.direi.tomash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/** Verifies version metadata is read from the Maven-filtered resource. */
class VersionInfoTest {

    /** Confirms the version resource contains the version declared by the project. */
    @Test
    void readsMavenProjectVersionFromFilteredResource() {
        assertEquals("2.0.0", VersionInfo.projectVersion());
    }
}