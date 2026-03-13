/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
import java.util.List;

import org.codedefenders.configuration.configfileresolver.StubConfigFileResolver;
import org.codedefenders.configuration.source.PropertiesFileSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PropertiesFileSourceTest {

    private PropertiesFileSource source1;
    private PropertiesFileSource source2;
    private PropertiesFileSource sourceMerged;

    @BeforeEach
    public void prepareObjects() {
        StubConfigFileResolver mCfgFileResolver1 = new StubConfigFileResolver() {
            @Override
            public int getPriority() {
                return 1;
            }
        };
        mCfgFileResolver1.setConfigFileContent("""
                cluster.timeout = 2
                db.username = testDatabaseUser
                block.attacker = true
                mutant.coverage = false""".stripIndent()
        );
        StubConfigFileResolver mCfgFileResolver2 = new StubConfigFileResolver() {
            @Override
            public int getPriority() {
                return 2;
            }
        };
        mCfgFileResolver2.setConfigFileContent("""
                cluster.timeout = 4
                db.password = 123456789
                block.attacker = disabled
                mutant.coverage = enabled""".stripIndent());

        source1 = new PropertiesFileSource(Arrays.asList(mCfgFileResolver1));
        source2 = new PropertiesFileSource(Arrays.asList(mCfgFileResolver2));
        sourceMerged = new PropertiesFileSource(Arrays.asList(mCfgFileResolver1, mCfgFileResolver2));
    }

    @Test
    public void lowerOnlyPropertyAccess() {
        assertEquals("testDatabaseUser", sourceMerged.resolveAttribute("dbUsername").orElse(null));
    }

    @Test
    public void upperOnlyPropertyAccess() {
        assertEquals("123456789", sourceMerged.resolveAttribute("dbPassword").orElse(null));
    }

    @Test
    public void overwrittenPropertyAccess() {
        assertEquals("4", sourceMerged.resolveAttribute("clusterTimeout").orElse(null));
    }

    @Test
    public void emptyStringShouldParseToNull() {
        StubConfigFileResolver mCfgFileResolverSetProp = new StubConfigFileResolver();
        mCfgFileResolverSetProp.setConfigFileContent(
                "cluster.timeout=4\n");

        StubConfigFileResolver mCfgFileResolverUnsetProp = new StubConfigFileResolver();
        mCfgFileResolverUnsetProp.setConfigFileContent(
                "cluster.timeout=\n");

        PropertiesFileSource source = new PropertiesFileSource(
                List.of(mCfgFileResolverSetProp, mCfgFileResolverUnsetProp));
        assertEquals("", source.resolveAttribute("clusterTimeout").orElse(null));
    }
}
