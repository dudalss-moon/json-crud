package com.crud.repository;

import com.crud.model.Item;
import com.jsonlib.Json;
import com.jsonlib.JsonArray;
import com.jsonlib.JsonObject;
import com.jsonlib.JsonValue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * JSON 파일을 영속 저장소로 사용하는 CRUD 리포지토리.
 * 데이터는 [{...}, {...}] 형태의 JSON 배열로 저장된다.
 */
public class JsonFileRepository {

    private final Path filePath;

    public JsonFileRepository(String filePath) {
        this.filePath = Path.of(filePath);
        initFile();
    }

    // ── Create ───────────────────────────────────────────────────────

    public Item create(JsonObject data) {
        JsonArray records = loadAll();
        long newId = nextId(records);
        data.put("id", newId);
        records.add(data);
        persist(records);
        return new Item(data);
    }

    // ── Read ─────────────────────────────────────────────────────────

    public List<Item> findAll() {
        JsonArray records = loadAll();
        List<Item> result = new ArrayList<>();
        for (JsonValue v : records) {
            result.add(new Item(v.asObject()));
        }
        return result;
    }

    public Optional<Item> findById(long id) {
        for (JsonValue v : loadAll()) {
            JsonObject obj = v.asObject();
            if (obj.getLong("id") == id) {
                return Optional.of(new Item(obj));
            }
        }
        return Optional.empty();
    }

    /**
     * 특정 키의 값이 keyword를 포함하는 항목을 검색한다 (대소문자 무시).
     */
    public List<Item> searchByField(String key, String keyword) {
        List<Item> result = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        for (JsonValue v : loadAll()) {
            JsonObject obj = v.asObject();
            if (!obj.has(key)) continue;
            JsonValue fieldVal = obj.get(key);
            if (fieldVal.isNull()) continue;
            String fieldStr = fieldVal.isString()
                    ? fieldVal.asString()
                    : fieldVal.toJsonString();
            if (fieldStr.toLowerCase().contains(lowerKeyword)) {
                result.add(new Item(obj));
            }
        }
        return result;
    }

    // ── Update ───────────────────────────────────────────────────────

    /**
     * id에 해당하는 항목의 특정 필드를 수정한다.
     * id 필드는 수정할 수 없다.
     */
    public boolean update(long id, String key, String value) {
        if ("id".equals(key)) return false;
        JsonArray records = loadAll();
        for (JsonValue v : records) {
            JsonObject obj = v.asObject();
            if (obj.getLong("id") == id) {
                obj.put(key, value);
                persist(records);
                return true;
            }
        }
        return false;
    }

    // ── Delete ───────────────────────────────────────────────────────

    public boolean delete(long id) {
        JsonArray records = loadAll();
        JsonArray updated = Json.array();
        boolean found = false;
        for (JsonValue v : records) {
            JsonObject obj = v.asObject();
            if (obj.getLong("id") == id) {
                found = true;
            } else {
                updated.add(obj);
            }
        }
        if (found) persist(updated);
        return found;
    }

    // ── 내부 유틸 ────────────────────────────────────────────────────

    private void initFile() {
        if (!Files.exists(filePath)) {
            persist(Json.array());
        }
    }

    private JsonArray loadAll() {
        return Json.loadArray(filePath);
    }

    private void persist(JsonArray records) {
        Json.savePretty(records, filePath);
    }

    private long nextId(JsonArray records) {
        long max = 0;
        for (JsonValue v : records) {
            long id = v.asObject().getLong("id");
            if (id > max) max = id;
        }
        return max + 1;
    }
}
