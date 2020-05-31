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

import org.codedefenders.configuration.configfileresolver.MockedConfigFileResolver;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TieredConfigurationTest {

    private TieredConfiguration config;

    @Before
    public void prepareObjects() {
        MockedConfigFileResolver configFile1 = new MockedConfigFileResolver();
        configFile1.setConfigFileContent(
                "cluster.timeout=4\n"
                        + "cluster.mode=enabled\n"
                        + "db.name=otherName\n"
                        + "force.local.execution=false");
        PropertiesFileConfiguration configPart1 = new PropertiesFileConfiguration(configFile1);
        configPart1.init();

        MockedConfigFileResolver configFile2 = new MockedConfigFileResolver();
        configFile2.setConfigFileContent(
                "cluster.timeout=8\n"
                        + "db.name=codedefenders\n"
                        + "db.username=testDatabaseUser\n"
                        + "force.local.execution=false");
        PropertiesFileConfiguration configPart2 = new PropertiesFileConfiguration(configFile2);
        configPart2.init();

        config = new TieredConfiguration(configPart1, configPart2);
        config.init();
    }

    @Test
    public void accessLoadedIntegerProperty() {
        assertEquals(8, config.getClusterTimeout());
    }

    @Test
    public void accessLoadedStringProperty() {
        assertEquals("testDatabaseUser", config.getDbUsername());
    }

    @Test
    public void accessLoadedBooleanProperty() {
        assertTrue(config.isClusterModeEnabled());
        assertFalse(config.isForceLocalExecution());
    }

    @Test
    public void overwriteWithDefaultValue() {
        // TODO: Could we make this more independent from our default values?
        assertEquals("jdbc:mysql://127.0.0.1:3306/codedefenders", config.getDbUrl());
    }
}
