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
 *
 * 파일 포맷: {"seq": N, "items": [{...}, ...]}
 *   - seq  : 지금까지 발급한 최대 ID (삭제 후에도 감소하지 않아 ID 재사용을 방지)
 *   - items: 현재 저장된 데이터 배열
 */
public class JsonFileRepository {

    private static final String KEY_SEQ   = "seq";
    private static final String KEY_ITEMS = "items";

    private final Path filePath;

    public JsonFileRepository(String filePath) {
        this.filePath = Path.of(filePath);
        initFile();
    }

    // ── Create ───────────────────────────────────────────────────────

    public Item create(JsonObject data) {
        JsonObject wrapper = loadWrapper();
        JsonArray  items   = wrapper.getArray(KEY_ITEMS);
        long       newId   = wrapper.getLong(KEY_SEQ) + 1;

        data.put("id", newId);
        items.add(data);
        wrapper.put(KEY_SEQ, newId);
        saveWrapper(wrapper);
        return new Item(data);
    }

    // ── Read ─────────────────────────────────────────────────────────

    public List<Item> findAll() {
        List<Item> result = new ArrayList<>();
        for (JsonValue v : loadWrapper().getArray(KEY_ITEMS)) {
            result.add(new Item(v.asObject()));
        }
        return result;
    }

    public Optional<Item> findById(long id) {
        for (JsonValue v : loadWrapper().getArray(KEY_ITEMS)) {
            JsonObject obj = v.asObject();
            if (obj.getLong("id") == id) return Optional.of(new Item(obj));
        }
        return Optional.empty();
    }

    /**
     * 특정 키의 값이 keyword를 포함하는 항목을 검색한다 (대소문자 무시).
     */
    public List<Item> searchByField(String key, String keyword) {
        List<Item> result = new ArrayList<>();
        String lower = keyword.toLowerCase();
        for (JsonValue v : loadWrapper().getArray(KEY_ITEMS)) {
            JsonObject obj = v.asObject();
            if (!obj.has(key)) continue;
            JsonValue fieldVal = obj.get(key);
            if (fieldVal.isNull()) continue;
            String fieldStr = fieldVal.isString() ? fieldVal.asString() : fieldVal.toJsonString();
            if (fieldStr.toLowerCase().contains(lower)) result.add(new Item(obj));
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
        JsonObject wrapper = loadWrapper();
        for (JsonValue v : wrapper.getArray(KEY_ITEMS)) {
            JsonObject obj = v.asObject();
            if (obj.getLong("id") == id) {
                obj.put(key, value);
                saveWrapper(wrapper);
                return true;
            }
        }
        return false;
    }

    // ── Delete ───────────────────────────────────────────────────────

    public boolean delete(long id) {
        JsonObject wrapper  = loadWrapper();
        JsonArray  items    = wrapper.getArray(KEY_ITEMS);
        JsonArray  filtered = Json.array();
        boolean    found    = false;

        for (JsonValue v : items) {
            JsonObject obj = v.asObject();
            if (obj.getLong("id") == id) found = true;
            else filtered.add(obj);
        }
        if (found) {
            wrapper.put(KEY_ITEMS, filtered);
            saveWrapper(wrapper);
        }
        return found;
    }

    // ── 내부 유틸 ────────────────────────────────────────────────────

    private void initFile() {
        if (!Files.exists(filePath)) {
            saveWrapper(emptyWrapper());
        }
    }

    private JsonObject loadWrapper() {
        return Json.loadObject(filePath);
    }

    private void saveWrapper(JsonObject wrapper) {
        Json.savePretty(wrapper, filePath);
    }

    private static JsonObject emptyWrapper() {
        return Json.object()
                .put(KEY_SEQ, 0L)
                .put(KEY_ITEMS, Json.array());
    }
}
