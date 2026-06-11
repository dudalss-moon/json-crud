package com.jsonlib;

public class JsonBoolean extends JsonValue {

    public static final JsonBoolean TRUE  = new JsonBoolean(true);
    public static final JsonBoolean FALSE = new JsonBoolean(false);

    private final boolean value;

    private JsonBoolean(boolean value) {
        this.value = value;
    }

    public static JsonBoolean of(boolean value) {
        return value ? TRUE : FALSE;
    }

    @Override public boolean isBoolean() { return true; }

    @Override
    public boolean asBoolean() { return value; }

    @Override
    public String toJsonString() { return value ? "true" : "false"; }

    @Override
    String toPrettyString(int indent) { return toJsonString(); }

    @Override
    public boolean equals(Object o) {
        return o instanceof JsonBoolean && value == ((JsonBoolean) o).value;
    }

    @Override
    public int hashCode() { return Boolean.hashCode(value); }
}
