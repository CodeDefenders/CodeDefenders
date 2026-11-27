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
package org.codedefenders.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class I18nServiceTest {
    private final I18nService service = new I18nService(null, null);

    @Test
    void preservesNullAndEmptyStrings() {
        assertNull(service.escape(null));
        assertNull(service.unescape(null));
        assertEquals("", service.escape(""));
    }

    @Test
    void escapesDelWithoutChangingLiteralUnicodeEscapes() {
        assertEquals("before\\u007Fafter", service.escape("before\u007fafter"));
        assertEquals("\\\\u007F", service.escape("\\u007F"));
        assertEquals("\u007f", service.unescape(service.escape("\u007f")));
    }

    @Test
    void escapesJavascriptStringContent() {
        assertEquals("\\\"\\\\\\n\\t\\/", service.escape("\"\\\n\t/"));
        assertEquals("\\uD83D\\uDE00", service.escape("\uD83D\uDE00"));
    }
}
