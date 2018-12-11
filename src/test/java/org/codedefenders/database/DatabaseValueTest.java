package org.codedefenders.database;

import org.codedefenders.model.Dependency;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Timestamp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

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
        final DatabaseValue<Integer> dbv = new DatabaseValue<>(value);
        assertEquals(Void.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue());
    }

    @Test
    public void testNullDatabaseValues2() {
        final String value = null;
        final DatabaseValue<String> dbv = new DatabaseValue<>(value);
        assertEquals(Void.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue());
    }

    @Test
    public void testIntegerDatabaseValues() {
        final int value = Integer.MAX_VALUE;
        final DatabaseValue<Integer> dbv = new DatabaseValue<>(value);
        assertEquals(Integer.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue().intValue());
    }

    @Test
    public void testLongDatabaseValues() {
        final long value = Integer.MAX_VALUE;
        final DatabaseValue<Long> dbv = new DatabaseValue<>(value);
        assertEquals(Long.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue().intValue());
    }

    @Test
    public void testFloatDatabaseValues() {
        final float value = Float.MAX_VALUE;
        final DatabaseValue<Float> dbv = new DatabaseValue<>(value);
        assertEquals(Float.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue(), 0.0);
    }

    @Test
    public void testStringDatabaseValues() {
        final String value = "Manuel Neuer";
        final DatabaseValue<String> dbv = new DatabaseValue<>(value);
        assertEquals(String.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue());
    }

    @Test
    public void testTimestampDatabaseValues() {
        final Timestamp value = new Timestamp(System.currentTimeMillis());
        final DatabaseValue<Timestamp> dbv = new DatabaseValue<>(value);
        assertEquals(Timestamp.class, dbv.getType().clazz);
        assertEquals(value, dbv.getValue());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongDatabaseValueType() {
        final Dependency value = new Dependency(1, 1, "", "");
        final DatabaseValue<Dependency> dbv = new DatabaseValue<>(value);

        assertNotEquals(Dependency.class, dbv.getType().clazz);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongDatabaseValueType2() {
        final SQLException value = new SQLException();
        final DatabaseValue<SQLException> dbv = new DatabaseValue<>(value);

        assertNotEquals(SQLException.class, dbv.getType().clazz);
    }
}
