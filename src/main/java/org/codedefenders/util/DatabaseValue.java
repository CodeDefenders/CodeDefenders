package org.codedefenders.util;

public class DatabaseValue {
    private Integer intVal;
    private Long longVal;
    private String stringVal;
    private Type type;

    public enum Type {
        LONG, INT, STRING
    }

    public DatabaseValue(String v) {
        this.type = Type.STRING;
        stringVal = v;
    }

    public DatabaseValue(int v) {
        this.type = Type.INT;
        intVal = v;
    }

    public DatabaseValue(Long v) {
        this.type = Type.LONG;
        longVal = v;
    }



    public Type getType() {
        return this.type;
    }

    public String getStringVal() {
        return this.stringVal;
    }

    public Integer getIntVal() {
        return this.intVal;
    }

    public Long getLongVal() {
        return this.longVal;
    }
}