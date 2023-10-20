/*
 * Copyright (C) 2020 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */

package org.codedefenders.configuration.implementation;

import java.util.Arrays;

import org.codedefenders.configuration.configfileresolver.StubConfigFileResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PropertiesFileConfigurationTest {

    private PropertiesFileConfiguration config1;
    private PropertiesFileConfiguration config2;
    private PropertiesFileConfiguration configMerged;

    @BeforeEach
    public void prepareObjects() {
        StubConfigFileResolver mCfgFileResolver1 = new StubConfigFileResolver();
        mCfgFileResolver1.setConfigFileContent("""
                cluster.timeout = 2
                db.username = testDatabaseUser
                block.attacker = true
                mutant.coverage = false""".stripIndent()
        );
        StubConfigFileResolver mCfgFileResolver2 = new StubConfigFileResolver();
        mCfgFileResolver2.setConfigFileContent("""
                cluster.timeout = 4
                db.password = 123456789
                block.attacker = disabled
                mutant.coverage = enabled""".stripIndent());

        config1 = new PropertiesFileConfiguration(Arrays.asList(mCfgFileResolver1));
        config1.init();

        config2 = new PropertiesFileConfiguration(Arrays.asList(mCfgFileResolver2));
        config2.init();

        configMerged = new PropertiesFileConfiguration(Arrays.asList(mCfgFileResolver1, mCfgFileResolver2));
        configMerged.init();
    }

    @Test
    public void lowerOnlyPropertyAccess() {
        assertEquals("testDatabaseUser", configMerged.getDbUsername());
    }

    @Test
    public void upperOnlyPropertyAccess() {
        assertEquals("123456789", configMerged.getDbPassword());
    }

    @Test
    public void overwrittenPropertyAccess() {
        assertEquals(4, configMerged.getClusterTimeout());
    }

    @Test
    public void simpleBooleanParsingTrue() {
        assertTrue(config1.isBlockAttacker());
    }

    @Test
    public void simpleBooleanParsingFalse() {
        assertFalse(config1.isMutantCoverage());
    }

    @Test
    public void legacyBooleanParsingTrue() {
        assertTrue(config2.isMutantCoverage());
    }

    @Test
    public void legacyBooleanParsingFalse() {
        assertFalse(config2.isBlockAttacker());
    }

    @Test
    public void emptyStringShouldParseToNull() {
        StubConfigFileResolver mCfgFileResolverSetProp = new StubConfigFileResolver();
        mCfgFileResolverSetProp.setConfigFileContent(
                "cluster.timeout=4\n");

        StubConfigFileResolver mCfgFileResolverUnsetProp = new StubConfigFileResolver();
        mCfgFileResolverUnsetProp.setConfigFileContent(
                "cluster.timeout=\n");

        PropertiesFileConfiguration configWithOverWritten = new PropertiesFileConfiguration(Arrays.asList(mCfgFileResolverSetProp, mCfgFileResolverUnsetProp));
        configWithOverWritten.init();

        assertEquals(-1, configWithOverWritten.getClusterTimeout());
    }
}
