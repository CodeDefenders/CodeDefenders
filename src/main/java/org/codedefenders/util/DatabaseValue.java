package org.codedefenders.util;

import java.sql.Timestamp;

public class DatabaseValue {
    private Integer intVal;
    private Long longVal;
    private String stringVal;
    private Type type;
    private float floatVal;
     private Timestamp timestampVal;

    public enum Type {
        LONG, INT, STRING, FLOAT, TIMESTAMP
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

    DatabaseValue(Long v) {
        this.type = Type.LONG;
        longVal = v;
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