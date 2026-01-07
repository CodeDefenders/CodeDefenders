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
import java.util.Optional;

import org.codedefenders.configuration.source.ConfigurationSource;
import org.codedefenders.configuration.source.TieredSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TieredConfigurationTest {

    private TieredSource config;

    @BeforeEach
    public void prepareObjects() {
        ConfigurationSource config1 = new ConfigurationSource() {
            @Override
            public Optional<String> resolveAttribute(String camelCaseName) {
                switch (camelCaseName) {
                    case "clusterTimeout":
                        return Optional.of("2");
                    case "dbUsername":
                        return Optional.of("testDatabaseUser");
                    case "blockAttacker":
                        return Optional.of("true");
                    case "mutantCoverage":
                        return Optional.of("false");
                    default:
                        return Optional.empty();
                }
            }

            @Override
            public int getPriority() {
                return 1;
            }
        };

        ConfigurationSource config2 = new ConfigurationSource() {
            @Override
            public Optional<String> resolveAttribute(String camelCaseName) {
                switch (camelCaseName) {
                    case "clusterTimeout":
                        return Optional.of("4");
                    case "dbPassword":
                        return Optional.of("123456789");
                    case "blockAttacker":
                        return Optional.of("false");
                    case "mutantCoverage":
                        return Optional.of("true");
                    default:
                        return Optional.empty();
                }
            }

            @Override
            public int getPriority() {
                return 2;
            }
        };

        config = new TieredSource(Arrays.asList(config1, config2));
    }

    @Test
    public void lowerOnlyPropertyAccess() {
        assertEquals("testDatabaseUser", config.resolveAttribute("dbUsername").orElse(null));
    }

    @Test
    public void upperOnlyPropertyAccess() {
        assertEquals("123456789", config.resolveAttribute("dbPassword").orElse(null));
    }

    @Test
    public void overwrittenPropertyAccess() {
        assertEquals("4", config.resolveAttribute("clusterTimeout").orElse(null));
    }

}
