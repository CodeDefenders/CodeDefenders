package org.codedefenders.database;

import org.codedefenders.model.Dependency;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests the {@link DatabaseValue} implementation for {@code null} values
 * and all possible types as well as impossible types.
 *
 * @author <a href="https://github.com/werli">Phil Werli<a/>
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

    @Test(expected = IllegalArgumentException.class)
    public void testWrongDatabaseValueType() {
        final Dependency value = new Dependency(1, 1, "", "");
        try {
            final Constructor<DatabaseValue> constructor = DatabaseValue.class.getDeclaredConstructor(Object.class);
            constructor.setAccessible(true);
            final DatabaseValue dbv = constructor.newInstance(value);
            fail("Should not successfully instantiate DatabaseValue for unsupported type: " + value.getClass().getName());
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IllegalArgumentException) {
                // cause is the IllegalArgumentException thrown by DatabaseValue.Type#get(Object)
                throw ((IllegalArgumentException) cause);
            }
            fail("No exception but the expected should be thrown:" + cause.getMessage());
        }

    }
}
