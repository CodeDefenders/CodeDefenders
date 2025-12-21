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
package org.codedefenders.configuration;


import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class ConfigurationTest {

    Configuration config;

    @BeforeEach
    public void prepareObjects() {
        // Setup configuration which will pass
        config = new Configuration(camelCaseName -> Optional.empty());
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

    @Test
    public void testGetAuthAdminUsers() {
        config.authAdminUsers = "userA,userB,userC";
        assertThat(config.getAuthAdminUsers())
                .containsExactly("userA", "userB", "userC");
    }

    @Test
    public void testGetAuthAdminUsersBadFormatting() {
        String someSymbols = ".;/\\@!?#";
        config.authAdminUsers = "userA   ,  userB,  , userC," + someSymbols;
        assertThat(config.getAuthAdminUsers())
                .containsExactly("userA", "userB", "userC", someSymbols);
    }

    @Test
    public void testCorrectTypeCoercion() {
        assertEquals(true, config.coerceType("", Boolean.class, "true").orElse(null));
        assertEquals(true, config.coerceType("", Boolean.class, "enabled").orElse(null));
        assertEquals(false, config.coerceType("", Boolean.class, "false").orElse(null));
        assertEquals(false, config.coerceType("", Boolean.class, "disabled").orElse(null));

        assertEquals(123, config.coerceType("", Integer.class, "123").orElse(null));
        assertEquals(-234, config.coerceType("", Integer.class, "-234").orElse(null));

        assertEquals("123", config.coerceType("", String.class, "123").orElse(null));
        assertEquals("-234", config.coerceType("", String.class, "-234").orElse(null));

        assertTrue(config.coerceType("", Boolean.class, "").isEmpty());
        assertTrue(config.coerceType("", Integer.class, "").isEmpty());
        assertTrue(config.coerceType("", String.class, "").isEmpty());
    }
}
