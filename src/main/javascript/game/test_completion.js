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
export function getTestMethods (assertionLibrary, mockingEnabled) {
    let testMethods = [];

    if (assertionLibrary.includes('JUNIT4')) {
        testMethods = testMethods.concat([
                'assertArrayEquals', 'assertEquals', 'assertFalse', 'assertNotEquals',
                'assertNotNull', 'assertNotSame', 'assertNull', 'assertSame',
                'assertThrows', 'assertTrue', 'fail'
        ]);
    }

    if (assertionLibrary.includes('JUNIT5')) {
        testMethods = testMethods.concat([
                'assertAll', 'assertArrayEquals', 'assertDoesNotThrow', 'assertEquals',
                'assertFalse', 'assertInstanceOf', 'assertIterableEquals',
                'assertLinesMatch', 'assertNotEquals', 'assertNotNull', 'assertNotSame',
                'assertNull', 'assertSame', 'assertThrows', 'assertThrowsExactly',
                'assertTrue', 'fail'
        ]);
    }

    if (assertionLibrary.includes('HAMCREST') || assertionLibrary.includes('GOOGLE_TRUTH')) {
        testMethods.push('assertThat');
    }

    if (mockingEnabled) {
        testMethods = testMethods.concat([
                'mock', 'when', 'then', 'thenThrow', 'doThrow', 'doReturn', 'doNothing'
        ]);
    }

    return testMethods;
}
