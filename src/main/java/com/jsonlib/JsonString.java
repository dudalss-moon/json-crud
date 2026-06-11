package com.jsonlib;

public class JsonString extends JsonValue {

    private final String value;

    public JsonString(String value) {
        if (value == null) throw new JsonException("JsonString value must not be null");
        this.value = value;
    }

    @Override public boolean isString() { return true; }

    @Override
    public String asString() { return value; }

    @Override
    public String toJsonString() {
        return escape(value);
    }

    @Override
    String toPrettyString(int indent) {
        return toJsonString();
    }

    static String escape(String raw) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append("\"").toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JsonString && value.equals(((JsonString) o).value);
    }

    @Override
    public int hashCode() { return value.hashCode(); }
}
