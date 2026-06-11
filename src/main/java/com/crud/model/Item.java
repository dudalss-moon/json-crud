package com.crud.model;

import com.jsonlib.JsonObject;
import com.jsonlib.JsonValue;

/**
 * JSON 파일에 저장되는 단일 데이터 항목.
 * 내부적으로 JsonObject를 그대로 사용하여 PoC 라이브러리 구조를 유지한다.
 */
public class Item {

    private final JsonObject data;

    public Item(JsonObject data) {
        this.data = data;
    }

    public long getId() {
        return data.getLong("id");
    }

    public String getFieldAsString(String key) {
        if (!data.has(key)) return null;
        JsonValue v = data.get(key);
        if (v.isNull()) return null;
        if (v.isString()) return v.asString();
        if (v.isNumber()) return String.valueOf(v.asDouble());
        if (v.isBoolean()) return String.valueOf(v.asBoolean());
        return v.toJsonString();
    }

    public JsonObject toJsonObject() {
        return data;
    }

    /** 특정 필드를 문자열 값으로 덮어쓴다. */
    public void setField(String key, String value) {
        data.put(key, value);
    }

    @Override
    public String toString() {
        return data.toPrettyString();
    }
}
