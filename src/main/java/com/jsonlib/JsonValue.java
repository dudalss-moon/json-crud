package com.jsonlib;

public abstract class JsonValue {

    public boolean isObject()  { return false; }
    public boolean isArray()   { return false; }
    public boolean isString()  { return false; }
    public boolean isNumber()  { return false; }
    public boolean isBoolean() { return false; }
    public boolean isNull()    { return false; }

    public JsonObject asObject() {
        throw new JsonException("Not a JSON object: " + getClass().getSimpleName());
    }

    public JsonArray asArray() {
        throw new JsonException("Not a JSON array: " + getClass().getSimpleName());
    }

    public String asString() {
        throw new JsonException("Not a JSON string: " + getClass().getSimpleName());
    }

    public double asDouble() {
        throw new JsonException("Not a JSON number: " + getClass().getSimpleName());
    }

    public long asLong() {
        throw new JsonException("Not a JSON number: " + getClass().getSimpleName());
    }

    public int asInt() {
        throw new JsonException("Not a JSON number: " + getClass().getSimpleName());
    }

    public boolean asBoolean() {
        throw new JsonException("Not a JSON boolean: " + getClass().getSimpleName());
    }

    public abstract String toJsonString();

    public String toPrettyString() {
        return toPrettyString(0);
    }

    abstract String toPrettyString(int indent);

    @Override
    public String toString() {
        return toJsonString();
    }
}
