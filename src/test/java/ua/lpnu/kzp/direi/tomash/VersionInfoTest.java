package ua.lpnu.kzp.direi.tomash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class VersionInfoTest {

    @Test
    void readsMavenProjectVersionFromFilteredResource() {
        assertEquals("1.0.0", VersionInfo.projectVersion());
    }
}