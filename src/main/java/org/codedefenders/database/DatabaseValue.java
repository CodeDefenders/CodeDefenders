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
 * along with Code Defenders.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.database;

import java.sql.Timestamp;

public class DatabaseValue {
    private Integer intVal;
    private Long longVal;
    private String stringVal;
    private Type type;
    private float floatVal;
    private Timestamp timestampVal;
    private Boolean boolVal;

    public enum Type {
        LONG, INT, STRING, FLOAT, TIMESTAMP, BOOLEAN
    }

    DatabaseValue(String v) {
        this.type = Type.STRING;
        stringVal = v;
    }

    DatabaseValue(float v) {
        this.type = Type.FLOAT;
        floatVal = v;
    }

    DatabaseValue(Timestamp v) {
        this.type = Type.TIMESTAMP;
        timestampVal = v;
    }

    DatabaseValue(int v) {
        this.type = Type.INT;
        intVal = v;
    }

    DatabaseValue(boolean v) {
        this.type = Type.BOOLEAN;
        boolVal = v;
    }

    DatabaseValue(Long v) {
        this.type = Type.LONG;
        longVal = v;
    }

	Boolean getBoolVal() {
		return this.boolVal;
	}

    public Type getType() {
        return this.type;
    }

    String getStringVal() {
        return this.stringVal;
    }

    Integer getIntVal() {
        return this.intVal;
    }

    Long getLongVal() {
        return this.longVal;
    }

    Float getFloatVal() {return this.floatVal;}

    Timestamp getTimestampVal() {return this.timestampVal;}
}