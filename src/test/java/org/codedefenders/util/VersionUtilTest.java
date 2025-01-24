package org.codedefenders.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class VersionUtilTest {

    @Test
    public void versionTest() {
        String result = VersionUtils.getCodeDefendersVersion();
        assertNotNull(result);
        assertTrue(result.matches("[0-9a-f]{7}"));
    }
}
