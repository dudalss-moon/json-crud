package com.jsonlib;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.file.Path;

/**
 * JSON 파싱 및 파일 저장을 위한 메인 퍼사드 클래스.
 *
 * 사용 예시:
 * <pre>
 *   // 파싱
 *   JsonObject obj = Json.parseObject("{\"name\":\"Alice\",\"age\":30}");
 *   String name = obj.getString("name");
 *
 *   // 생성
 *   JsonObject user = Json.object()
 *       .put("name", "Alice")
 *       .put("age", 30);
 *
 *   // 파일 저장 / 읽기
 *   Json.savePretty(user, "user.json");
 *   JsonObject loaded = Json.loadObject("user.json");
 * </pre>
 */
public final class Json {

    private Json() {}

    // ── 파싱 ─────────────────────────────────────────────────────────

    public static JsonValue parse(String json) {
        return JsonParser.parse(json);
    }

    public static JsonObject parseObject(String json) {
        return JsonParser.parseObject(json);
    }

    public static JsonArray parseArray(String json) {
        return JsonParser.parseArray(json);
    }

    // ── 타입 생성 헬퍼 ────────────────────────────────────────────────

    public static JsonObject object() {
        return new JsonObject();
    }

    public static JsonArray array() {
        return new JsonArray();
    }

    public static JsonString string(String value) {
        return value == null ? null : new JsonString(value);
    }

    public static JsonNumber number(Number value) {
        return value == null ? null : new JsonNumber(value);
    }

    public static JsonBoolean bool(boolean value) {
        return JsonBoolean.of(value);
    }

    public static JsonNull nil() {
        return JsonNull.INSTANCE;
    }

    // ── 직렬화 ───────────────────────────────────────────────────────

    public static String toJson(JsonValue value) {
        return JsonWriter.compact().toString(value);
    }

    public static String toPrettyJson(JsonValue value) {
        return JsonWriter.pretty().toString(value);
    }

    // ── 파일 저장 ─────────────────────────────────────────────────────

    public static void save(JsonValue value, String filePath) {
        JsonWriter.compact().write(value, filePath);
    }

    public static void save(JsonValue value, Path path) {
        JsonWriter.compact().write(value, path);
    }

    public static void save(JsonValue value, File file) {
        JsonWriter.compact().write(value, file);
    }

    public static void savePretty(JsonValue value, String filePath) {
        JsonWriter.pretty().write(value, filePath);
    }

    public static void savePretty(JsonValue value, Path path) {
        JsonWriter.pretty().write(value, path);
    }

    public static void savePretty(JsonValue value, File file) {
        JsonWriter.pretty().write(value, file);
    }

    public static void save(JsonValue value, OutputStream out) {
        JsonWriter.compact().write(value, out);
    }

    public static void savePretty(JsonValue value, OutputStream out) {
        JsonWriter.pretty().write(value, out);
    }

    public static void save(JsonValue value, Writer writer) {
        JsonWriter.compact().write(value, writer);
    }

    // ── 파일 읽기 ─────────────────────────────────────────────────────

    public static JsonValue load(String filePath) {
        return JsonWriter.readFrom(filePath);
    }

    public static JsonValue load(Path path) {
        return JsonWriter.readFrom(path);
    }

    public static JsonValue load(File file) {
        return JsonWriter.readFrom(file);
    }

    public static JsonValue load(InputStream in) {
        return JsonWriter.readFrom(in);
    }

    public static JsonObject loadObject(String filePath) {
        return JsonWriter.readFrom(filePath).asObject();
    }

    public static JsonObject loadObject(Path path) {
        return JsonWriter.readFrom(path).asObject();
    }

    public static JsonArray loadArray(String filePath) {
        return JsonWriter.readFrom(filePath).asArray();
    }

    public static JsonArray loadArray(Path path) {
        return JsonWriter.readFrom(path).asArray();
    }
}
