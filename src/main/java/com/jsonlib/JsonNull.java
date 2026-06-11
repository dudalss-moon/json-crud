package com.jsonlib;

public class JsonNull extends JsonValue {

    public static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {}

    @Override public boolean isNull() { return true; }

    @Override
    public String toJsonString() { return "null"; }

    @Override
    String toPrettyString(int indent) { return "null"; }

    @Override
    public boolean equals(Object o) { return o instanceof JsonNull; }

    @Override
    public int hashCode() { return 0; }
}
