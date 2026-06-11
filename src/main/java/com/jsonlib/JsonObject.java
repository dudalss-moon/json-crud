package com.jsonlib;

import java.util.*;

public class JsonObject extends JsonValue {

    private final LinkedHashMap<String, JsonValue> members = new LinkedHashMap<>();

    public JsonObject put(String key, JsonValue value) {
        members.put(key, value == null ? JsonNull.INSTANCE : value);
        return this;
    }

    public JsonObject put(String key, String value) {
        return put(key, value == null ? JsonNull.INSTANCE : new JsonString(value));
    }

    public JsonObject put(String key, Number value) {
        return put(key, value == null ? JsonNull.INSTANCE : new JsonNumber(value));
    }

    public JsonObject put(String key, Boolean value) {
        return put(key, value == null ? JsonNull.INSTANCE : JsonBoolean.of(value));
    }

    public JsonValue get(String key) {
        return members.get(key);
    }

    public String getString(String key) {
        JsonValue v = require(key);
        return v.isNull() ? null : v.asString();
    }

    public double getDouble(String key) {
        return require(key).asDouble();
    }

    public long getLong(String key) {
        return require(key).asLong();
    }

    public int getInt(String key) {
        return require(key).asInt();
    }

    public boolean getBoolean(String key) {
        return require(key).asBoolean();
    }

    public JsonObject getObject(String key) {
        JsonValue v = require(key);
        return v.isNull() ? null : v.asObject();
    }

    public JsonArray getArray(String key) {
        JsonValue v = require(key);
        return v.isNull() ? null : v.asArray();
    }

    public boolean has(String key) {
        return members.containsKey(key);
    }

    public boolean isNull(String key) {
        JsonValue v = members.get(key);
        return v == null || v.isNull();
    }

    public JsonObject remove(String key) {
        members.remove(key);
        return this;
    }

    public Set<String> keys() {
        return Collections.unmodifiableSet(members.keySet());
    }

    public Collection<JsonValue> values() {
        return Collections.unmodifiableCollection(members.values());
    }

    public Set<Map.Entry<String, JsonValue>> entries() {
        return Collections.unmodifiableSet(members.entrySet());
    }

    public int size() {
        return members.size();
    }

    public boolean isEmpty() {
        return members.isEmpty();
    }

    @Override public boolean isObject() { return true; }

    @Override
    public JsonObject asObject() { return this; }

    @Override
    public String toJsonString() {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, JsonValue> entry : members.entrySet()) {
            if (!first) sb.append(",");
            sb.append(JsonString.escape(entry.getKey()));
            sb.append(":");
            sb.append(entry.getValue().toJsonString());
            first = false;
        }
        return sb.append("}").toString();
    }

    @Override
    String toPrettyString(int indent) {
        if (members.isEmpty()) return "{}";
        String pad = "  ".repeat(indent + 1);
        String closePad = "  ".repeat(indent);
        StringBuilder sb = new StringBuilder("{\n");
        boolean first = true;
        for (Map.Entry<String, JsonValue> entry : members.entrySet()) {
            if (!first) sb.append(",\n");
            sb.append(pad).append(JsonString.escape(entry.getKey())).append(": ");
            sb.append(entry.getValue().toPrettyString(indent + 1));
            first = false;
        }
        return sb.append("\n").append(closePad).append("}").toString();
    }

    private JsonValue require(String key) {
        if (!members.containsKey(key)) {
            throw new JsonException("Key not found: " + key);
        }
        return members.get(key);
    }
}
