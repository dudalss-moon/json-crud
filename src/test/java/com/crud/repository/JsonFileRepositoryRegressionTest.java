package com.crud.repository;

import com.crud.model.Item;
import com.jsonlib.Json;
import com.jsonlib.JsonObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonFileRepository - 회귀(Regression) 테스트")
class JsonFileRepositoryRegressionTest {

    @TempDir
    Path tempDir;

    private JsonFileRepository repo;

    @BeforeEach
    void setUp() {
        repo = new JsonFileRepository(tempDir.resolve("items.json").toString());
    }

    // ── Create ───────────────────────────────────────────────────────

    @Test
    @DisplayName("첫 번째 항목 생성 시 ID는 1")
    void create_firstItem_idIsOne() {
        Item item = repo.create(obj("name", "Alice"));
        assertEquals(1L, item.getId());
    }

    @Test
    @DisplayName("두 번째 항목 생성 시 ID는 2로 자동 증가")
    void create_secondItem_idIncrements() {
        repo.create(obj("name", "Alice"));
        Item second = repo.create(obj("name", "Bob"));
        assertEquals(2L, second.getId());
    }

    @Test
    @DisplayName("생성한 필드 값이 저장 후에도 유지됨")
    void create_fieldValuesPersistedCorrectly() {
        repo.create(obj("name", "Alice", "age", "30"));
        Item found = repo.findById(1L).orElseThrow();
        assertEquals("Alice", found.getFieldAsString("name"));
        assertEquals("30",    found.getFieldAsString("age"));
    }

    @Test
    @DisplayName("생성한 항목이 파일에서 다시 읽어도 동일")
    void create_persistsAcrossRepoReload() {
        repo.create(obj("name", "Alice"));
        JsonFileRepository reloaded = new JsonFileRepository(
                tempDir.resolve("items.json").toString());
        assertEquals(1, reloaded.findAll().size());
        assertEquals("Alice", reloaded.findById(1L).orElseThrow().getFieldAsString("name"));
    }

    // ── Read All ─────────────────────────────────────────────────────

    @Test
    @DisplayName("저장된 항목이 없으면 빈 리스트 반환")
    void findAll_emptyStore_returnsEmptyList() {
        assertTrue(repo.findAll().isEmpty());
    }

    @Test
    @DisplayName("생성한 모든 항목이 목록에 포함됨")
    void findAll_returnsAllCreatedItems() {
        repo.create(obj("name", "Alice"));
        repo.create(obj("name", "Bob"));
        repo.create(obj("name", "Charlie"));
        assertEquals(3, repo.findAll().size());
    }

    @Test
    @DisplayName("findAll 반환 순서는 삽입 순서와 동일")
    void findAll_preservesInsertionOrder() {
        repo.create(obj("name", "Alice"));
        repo.create(obj("name", "Bob"));
        List<Item> all = repo.findAll();
        assertEquals("Alice", all.get(0).getFieldAsString("name"));
        assertEquals("Bob",   all.get(1).getFieldAsString("name"));
    }

    // ── Find by ID ───────────────────────────────────────────────────

    @Test
    @DisplayName("존재하는 ID 조회 시 해당 항목 반환")
    void findById_existingId_returnsItem() {
        repo.create(obj("name", "Alice"));
        Optional<Item> result = repo.findById(1L);
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getFieldAsString("name"));
    }

    @Test
    @DisplayName("존재하지 않는 ID 조회 시 Optional.empty() 반환")
    void findById_missingId_returnsEmpty() {
        assertTrue(repo.findById(999L).isEmpty());
    }

    @Test
    @DisplayName("삭제 후 해당 ID 조회 시 Optional.empty() 반환")
    void findById_afterDelete_returnsEmpty() {
        repo.create(obj("name", "Alice"));
        repo.delete(1L);
        assertTrue(repo.findById(1L).isEmpty());
    }

    // ── Search by Field ──────────────────────────────────────────────

    @Test
    @DisplayName("필드 값과 완전히 일치하는 항목 검색")
    void searchByField_exactMatch_returnsItem() {
        repo.create(obj("name", "Alice"));
        repo.create(obj("name", "Bob"));
        List<Item> result = repo.searchByField("name", "Alice");
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getFieldAsString("name"));
    }

    @Test
    @DisplayName("필드 값에 검색어가 포함된 모든 항목 반환 (부분 일치)")
    void searchByField_partialMatch_returnsItems() {
        repo.create(obj("name", "Alice"));
        repo.create(obj("name", "Alicia"));
        repo.create(obj("name", "Bob"));
        List<Item> result = repo.searchByField("name", "Ali");
        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("검색은 대소문자를 구별하지 않음")
    void searchByField_caseInsensitive() {
        repo.create(obj("name", "Alice"));
        assertEquals(1, repo.searchByField("name", "alice").size());
        assertEquals(1, repo.searchByField("name", "ALICE").size());
        assertEquals(1, repo.searchByField("name", "AlIcE").size());
    }

    @Test
    @DisplayName("검색 결과가 없으면 빈 리스트 반환")
    void searchByField_noMatch_returnsEmptyList() {
        repo.create(obj("name", "Alice"));
        assertTrue(repo.searchByField("name", "Zara").isEmpty());
    }

    @Test
    @DisplayName("해당 필드가 없는 항목은 검색 결과에 포함되지 않음")
    void searchByField_itemMissingField_excluded() {
        repo.create(obj("name", "Alice"));           // name 필드 있음
        repo.create(obj("email", "b@b.com"));        // name 필드 없음
        List<Item> result = repo.searchByField("name", "Alice");
        assertEquals(1, result.size());
    }

    // ── Update ───────────────────────────────────────────────────────

    @Test
    @DisplayName("기존 필드 수정 후 값이 변경됨")
    void update_existingField_valueChanges() {
        repo.create(obj("name", "Alice", "age", "30"));
        repo.update(1L, "age", "31");
        assertEquals("31", repo.findById(1L).orElseThrow().getFieldAsString("age"));
    }

    @Test
    @DisplayName("수정 성공 시 true 반환")
    void update_success_returnsTrue() {
        repo.create(obj("name", "Alice"));
        assertTrue(repo.update(1L, "name", "Alicia"));
    }

    @Test
    @DisplayName("수정이 다른 필드에 영향을 주지 않음")
    void update_doesNotAffectOtherFields() {
        repo.create(obj("name", "Alice", "age", "30"));
        repo.update(1L, "age", "31");
        assertEquals("Alice", repo.findById(1L).orElseThrow().getFieldAsString("name"));
    }

    @Test
    @DisplayName("수정 내용이 파일에 영구 저장됨")
    void update_persistsAcrossReload() {
        repo.create(obj("name", "Alice"));
        repo.update(1L, "name", "Alicia");
        JsonFileRepository reloaded = new JsonFileRepository(
                tempDir.resolve("items.json").toString());
        assertEquals("Alicia", reloaded.findById(1L).orElseThrow().getFieldAsString("name"));
    }

    @Test
    @DisplayName("존재하지 않는 ID 수정 시 false 반환")
    void update_missingId_returnsFalse() {
        assertFalse(repo.update(999L, "name", "Ghost"));
    }

    @Test
    @DisplayName("새 필드를 추가하는 형태의 update도 동작")
    void update_newField_addsField() {
        repo.create(obj("name", "Alice"));
        repo.update(1L, "email", "alice@test.com");
        assertEquals("alice@test.com",
                repo.findById(1L).orElseThrow().getFieldAsString("email"));
    }

    // ── Delete ───────────────────────────────────────────────────────

    @Test
    @DisplayName("삭제 성공 시 true 반환")
    void delete_success_returnsTrue() {
        repo.create(obj("name", "Alice"));
        assertTrue(repo.delete(1L));
    }

    @Test
    @DisplayName("삭제 후 전체 목록에서 제거됨")
    void delete_removesFromFindAll() {
        repo.create(obj("name", "Alice"));
        repo.create(obj("name", "Bob"));
        repo.delete(1L);
        List<Item> all = repo.findAll();
        assertEquals(1, all.size());
        assertEquals("Bob", all.get(0).getFieldAsString("name"));
    }

    @Test
    @DisplayName("삭제 내용이 파일에 영구 반영됨")
    void delete_persistsAcrossReload() {
        repo.create(obj("name", "Alice"));
        repo.delete(1L);
        JsonFileRepository reloaded = new JsonFileRepository(
                tempDir.resolve("items.json").toString());
        assertTrue(reloaded.findAll().isEmpty());
    }

    @Test
    @DisplayName("존재하지 않는 ID 삭제 시 false 반환")
    void delete_missingId_returnsFalse() {
        assertFalse(repo.delete(999L));
    }

    @Test
    @DisplayName("삭제 후 새 항목 생성 시 ID는 이전 최댓값+1")
    void delete_thenCreate_idContinuesFromMax() {
        repo.create(obj("name", "Alice")); // id=1
        repo.create(obj("name", "Bob"));   // id=2
        repo.delete(2L);
        Item next = repo.create(obj("name", "Charlie"));
        assertEquals(3L, next.getId());    // 2를 재사용하지 않음
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────

    private static JsonObject obj(String... kv) {
        JsonObject o = Json.object();
        for (int i = 0; i < kv.length; i += 2) o.put(kv[i], kv[i + 1]);
        return o;
    }
}
