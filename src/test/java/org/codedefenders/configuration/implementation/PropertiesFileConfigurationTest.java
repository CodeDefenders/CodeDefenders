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

import org.codedefenders.configuration.ConfigurationValidationException;
import org.codedefenders.configuration.configfileresolver.StubConfigFileResolver;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertiesFileConfigurationTest {

    private PropertiesFileConfiguration config;

    @Before
    public void prepareObjects() {
        StubConfigFileResolver mCfgFileResolver = new StubConfigFileResolver();
        mCfgFileResolver.setConfigFileContent(
                "cluster.timeout=4\n"
                        + "db.username=testDatabaseUser\n"
                        + "cluster.mode=enabled\n"
                        + "force.local.execution=false");

        config = new PropertiesFileConfiguration(new ArrayList<>(Arrays.asList(mCfgFileResolver)));
        config.init();
    }

    @Test
    public void accessLoadedIntegerProperty() {
        assertEquals(4, config.getClusterTimeout());
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

    @Test(expected = ConfigurationValidationException.class)
    public void loadInvalidConfig() throws ConfigurationValidationException {
        StubConfigFileResolver mCfgFileResolver = new StubConfigFileResolver();
        mCfgFileResolver.setConfigFileContent("db.host=157.1646846.456.568\n"
                + "db.port=65537");

        config = new PropertiesFileConfiguration(new ArrayList<>(Arrays.asList(mCfgFileResolver)));
        config.init();
        config.validate();
    }
}
