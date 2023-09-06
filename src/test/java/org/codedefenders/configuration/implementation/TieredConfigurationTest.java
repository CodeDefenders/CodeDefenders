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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TieredConfigurationTest {

    private TieredConfiguration config;

    @BeforeEach
    public void prepareObjects() {
        BaseConfiguration config1 = new BaseConfiguration() {
            @Override
            protected Object resolveAttribute(String camelCaseName) {
                switch (camelCaseName) {
                    case "clusterTimeout":
                        return 2;
                    case "dbUsername":
                        return "testDatabaseUser";
                    case "blockAttacker":
                        return true;
                    case "mutantCoverage":
                        return false;
                    default:
                        return null;
                }
            }
        };
        config1.init();

        BaseConfiguration config2 = new BaseConfiguration() {
            @Override
            protected Object resolveAttribute(String camelCaseName) {
                switch (camelCaseName) {
                    case "clusterTimeout":
                        return 4;
                    case "dbPassword":
                        return "123456789";
                    case "blockAttacker":
                        return false;
                    case "mutantCoverage":
                        return true;
                    default:
                        return null;
                }
            }
        };
        config2.init();

        config = new TieredConfiguration(Arrays.asList(config1, config2));
        config.init();
    }

    @Test
    public void lowerOnlyPropertyAccess() {
        assertEquals("testDatabaseUser", config.getDbUsername());
    }

    @Test
    public void upperOnlyPropertyAccess() {
        assertEquals("123456789", config.getDbPassword());
    }

    @Test
    public void overwrittenPropertyAccess() {
        assertEquals(4, config.getClusterTimeout());
    }

}
