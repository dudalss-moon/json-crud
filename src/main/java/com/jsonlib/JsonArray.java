package com.jsonlib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class JsonArray extends JsonValue implements Iterable<JsonValue> {

    private final List<JsonValue> elements = new ArrayList<>();

    public JsonArray add(JsonValue value) {
        elements.add(value == null ? JsonNull.INSTANCE : value);
        return this;
    }

    public JsonArray add(String value) {
        return add(value == null ? JsonNull.INSTANCE : new JsonString(value));
    }

    public JsonArray add(Number value) {
        return add(value == null ? JsonNull.INSTANCE : new JsonNumber(value));
    }

    public JsonArray add(Boolean value) {
        return add(value == null ? JsonNull.INSTANCE : JsonBoolean.of(value));
    }

    public JsonValue get(int index) {
        return elements.get(index);
    }

    public JsonArray set(int index, JsonValue value) {
        elements.set(index, value == null ? JsonNull.INSTANCE : value);
        return this;
    }

    public JsonArray remove(int index) {
        elements.remove(index);
        return this;
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public List<JsonValue> asList() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public Iterator<JsonValue> iterator() {
        return elements.iterator();
    }

    @Override public boolean isArray() { return true; }

    @Override
    public JsonArray asArray() { return this; }

    @Override
    public String toJsonString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(elements.get(i).toJsonString());
        }
        return sb.append("]").toString();
    }

    @Override
    String toPrettyString(int indent) {
        if (elements.isEmpty()) return "[]";
        String pad = "  ".repeat(indent + 1);
        String closePad = "  ".repeat(indent);
        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < elements.size(); i++) {
            if (i > 0) sb.append(",\n");
            sb.append(pad).append(elements.get(i).toPrettyString(indent + 1));
        }
        return sb.append("\n").append(closePad).append("]").toString();
    }
}
