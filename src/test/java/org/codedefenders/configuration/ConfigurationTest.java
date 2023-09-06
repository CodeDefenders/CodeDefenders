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

package org.codedefenders.configuration;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ConfigurationTest {

    Configuration config;

    @BeforeEach
    public void prepareObjects() {
        // Setup configuration which will pass
        config = new Configuration();
        config.dbPort = 3306;
        config.dbHost = "localhost";
    }

    @Test
    public void invalidDbHost() {
        config.dbHost = "157.1646846.456.568";

        try {
            config.validate();
            fail("Should throw exception.");
        } catch (ConfigurationValidationException e) {
            assertTrue(e.getMessage().contains("dbHost"));
        }
    }


    @Test
    public void invalidDbPort() {
        config.dbPort = 65537;

        try {
            config.validate();
            fail("Should throw exception.");
        } catch (ConfigurationValidationException e) {
            assertTrue(e.getMessage().contains("dbPort"));
        }
    }
}
