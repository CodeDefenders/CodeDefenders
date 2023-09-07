/*
 * Copyright (C) 2016-2019 Code Defenders contributors
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
package org.codedefenders.database;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;

import org.codedefenders.model.Dependency;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;


/**
 * Tests the {@link DatabaseValue} implementation for {@code null} values
 * and all possible types as well as impossible types.
 *
 * @author <a href="https://github.com/werli">Phil Werli</a>
 */
public class DatabaseValueTest {

    @Test
    public void testNullDatabaseValues() {
        final Integer value = null;
        final DatabaseValue<Integer> dbv = DatabaseValue.of(value);
        assertEquals(Void.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue());
    }

    @Test
    public void testNullDatabaseValues2() {
        final String value = null;
        final DatabaseValue<String> dbv = DatabaseValue.of(value);
        assertEquals(Void.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue());
    }

    @Test
    public void testIntegerDatabaseValues() {
        final int value = Integer.MAX_VALUE;
        final DatabaseValue<Integer> dbv = DatabaseValue.of(value);
        assertEquals(Integer.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue().intValue());
    }

    @Test
    public void testLongDatabaseValues() {
        final long value = Integer.MAX_VALUE;
        final DatabaseValue<Long> dbv = DatabaseValue.of(value);
        assertEquals(Long.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue().intValue());
    }

    @Test
    public void testFloatDatabaseValues() {
        final float value = Float.MAX_VALUE;
        final DatabaseValue<Float> dbv = DatabaseValue.of(value);
        assertEquals(Float.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue(), 0.0);
    }

    @Test
    public void testStringDatabaseValues() {
        final String value = "Manuel Neuer";
        final DatabaseValue<String> dbv = DatabaseValue.of(value);
        assertEquals(String.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue());
    }

    @Test
    public void testTimestampDatabaseValues() {
        final Timestamp value = new Timestamp(System.currentTimeMillis());
        final DatabaseValue<Timestamp> dbv = DatabaseValue.of(value);
        assertEquals(Timestamp.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue());
    }

    @Test
    public void testWrongDatabaseValueType() {
        assertThrows(IllegalArgumentException.class, () -> {
            final Dependency value = new Dependency(1, 1, "", "");
            try {
                // Disable inspection because the type parameter information of 'DatabaseValue' is not retained at runtime,
                // and so can't be used with reflection
                //noinspection rawtypes
                final Constructor<DatabaseValue> constructor = DatabaseValue.class.getDeclaredConstructor(Object.class);
                constructor.setAccessible(true);
                final DatabaseValue<?> dbv = constructor.newInstance(value);
                fail("Should not successfully instantiate DatabaseValue for unsupported type: " + value.getClass().getName());
            } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                final Throwable cause = e.getCause();
                if (cause instanceof IllegalArgumentException) {
                    // cause is the IllegalArgumentException thrown by DatabaseValue.Type#get(Object)
                    throw cause;
                }
                fail("No exception but the expected should be thrown:" + cause.getMessage());
            }
        });
    }
}
