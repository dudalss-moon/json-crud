package com.jsonlib;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonWriter {

    private final boolean pretty;
    private final String charset;

    private JsonWriter(boolean pretty, String charset) {
        this.pretty  = pretty;
        this.charset = charset;
    }

    public static JsonWriter compact() {
        return new JsonWriter(false, "UTF-8");
    }

    public static JsonWriter pretty() {
        return new JsonWriter(true, "UTF-8");
    }

    public JsonWriter withCharset(String charset) {
        return new JsonWriter(this.pretty, charset);
    }

    public String toString(JsonValue value) {
        if (value == null) return "null";
        return pretty ? value.toPrettyString() : value.toJsonString();
    }

    public void write(JsonValue value, Path path) {
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
        } catch (IOException e) {
            // parent already exists
        }
        try (BufferedWriter bw = Files.newBufferedWriter(path, java.nio.charset.Charset.forName(charset))) {
            bw.write(toString(value));
        } catch (IOException e) {
            throw new JsonException("Failed to write JSON to file: " + path, e);
        }
    }

    public void write(JsonValue value, String filePath) {
        write(value, Path.of(filePath));
    }

    public void write(JsonValue value, File file) {
        write(value, file.toPath());
    }

    public void write(JsonValue value, OutputStream out) {
        try {
            out.write(toString(value).getBytes(charset));
            out.flush();
        } catch (IOException e) {
            throw new JsonException("Failed to write JSON to stream", e);
        }
    }

    public void write(JsonValue value, Writer writer) {
        try {
            writer.write(toString(value));
            writer.flush();
        } catch (IOException e) {
            throw new JsonException("Failed to write JSON to writer", e);
        }
    }

    public static JsonValue readFrom(Path path) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            return JsonParser.parse(content);
        } catch (IOException e) {
            throw new JsonException("Failed to read JSON from file: " + path, e);
        }
    }

    public static JsonValue readFrom(String filePath) {
        return readFrom(Path.of(filePath));
    }

    public static JsonValue readFrom(File file) {
        return readFrom(file.toPath());
    }

    public static JsonValue readFrom(InputStream in) {
        try {
            byte[] bytes = in.readAllBytes();
            return JsonParser.parse(new String(bytes, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new JsonException("Failed to read JSON from stream", e);
        }
    }
}
