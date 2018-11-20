/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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

import java.sql.Timestamp;
import java.sql.Types;

/**
 * This class represents values which can be inserted into a SQL database.
 * <p>
 * Instances have a type and a value, which can be {@code null},
 * if a SQL {@code NULL} value is represented.
 */
public class DatabaseValue<T> {
    private Type type;
    private T value;

    DatabaseValue(T value) {
        this.type = Type.get(value);
        this.value = value;
    }

    /**
     * @return the {@link Type} enum instance of this {@link DatabaseValue}.
     */
    public Type getType() {
        return type;
    }

    /**
     * Retrieves the typed value of this {@link DatabaseValue}, or {@code null}.
     *
     * @return a typed value or {@code null}.
     */
    public T getValue() {
        return value;
    }

    enum Type {
        NULL(Types.NULL, Void.class),
        INT(Types.INTEGER, Integer.class),
        LONG(Types.BIGINT, Long.class),
        FLOAT(Types.FLOAT, Float.class),
        STRING(Types.VARCHAR, String.class),
        TIMESTAMP(Types.TIMESTAMP, Timestamp.class),
        BOOLEAN(Types.BOOLEAN, Boolean.class);

        int typeValue;
        Class<?> clazz;

        Type(int typeValue, Class clazz) {
            this.typeValue = typeValue;
            this.clazz = clazz;
        }

        static Type get(Object value) {
            if (value == null) {
                return NULL;
            }
            final Class<?> clazz = value.getClass();
            if (INT.clazz == clazz) {
                return INT;
            } else if (LONG.clazz == clazz) {
                return LONG;
            } else if (FLOAT.clazz == clazz) {
                return FLOAT;
            } else if (STRING.clazz == clazz) {
                return STRING;
            } else if (TIMESTAMP.clazz == clazz) {
                return TIMESTAMP;
            } else if (BOOLEAN.clazz == clazz) {
                return BOOLEAN;
            } else {
                throw new IllegalArgumentException("Tried to create database value for class " + clazz.getName() +
                        ", which is not supported!");
            }
        }
    }
}