package com.jsonlib;

public class JsonNumber extends JsonValue {

    private final Number value;

    public JsonNumber(Number value) {
        if (value == null) throw new JsonException("JsonNumber value must not be null");
        this.value = value;
    }

    @Override public boolean isNumber() { return true; }

    @Override public double asDouble() { return value.doubleValue(); }
    @Override public long   asLong()   { return value.longValue(); }
    @Override public int    asInt()    { return value.intValue(); }

    @Override
    public String toJsonString() {
        if (value instanceof Double || value instanceof Float) {
            double d = value.doubleValue();
            if (Double.isInfinite(d) || Double.isNaN(d)) {
                throw new JsonException("JSON does not support Infinite or NaN numbers");
            }
            if (d == Math.floor(d) && !Double.isInfinite(d) && Math.abs(d) < 1e15) {
                return String.valueOf((long) d);
            }
            return String.valueOf(d);
        }
        return String.valueOf(value);
    }

    @Override
    String toPrettyString(int indent) {
        return toJsonString();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof JsonNumber)) return false;
        return value.doubleValue() == ((JsonNumber) o).value.doubleValue();
    }

    @Override
    public int hashCode() { return Double.hashCode(value.doubleValue()); }
}
