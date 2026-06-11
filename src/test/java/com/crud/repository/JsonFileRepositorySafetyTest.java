package com.crud.repository;

import com.crud.model.Item;
import com.jsonlib.Json;
import com.jsonlib.JsonException;
import com.jsonlib.JsonObject;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonFileRepository - 안정성(Safety) 테스트")
class JsonFileRepositorySafetyTest {

    @TempDir
    Path tempDir;

    private JsonFileRepository repo;
    private Path dataFile;

    @BeforeEach
    void setUp() {
        dataFile = tempDir.resolve("items.json");
        repo = new JsonFileRepository(dataFile.toString());
    }

    // ── Create: 이상한 값 입력 ────────────────────────────────────────

    @Test
    @DisplayName("빈 문자열 값도 저장됨")
    void create_emptyStringValue_stored() {
        repo.create(obj("name", ""));
        assertEquals("", repo.findById(1L).orElseThrow().getFieldAsString("name"));
    }

    @Test
    @DisplayName("공백만 있는 값도 저장됨")
    void create_whitespaceOnlyValue_stored() {
        repo.create(obj("name", "   "));
        assertEquals("   ", repo.findById(1L).orElseThrow().getFieldAsString("name"));
    }

    @ParameterizedTest(name = "JSON 특수문자: {0}")
    @DisplayName("JSON 특수문자가 포함된 값도 저장·복원됨")
    @ValueSource(strings = {
        "He said \"hello\"",
        "C:\\Users\\test",
        "line1\nline2",
        "tab\there",
        "{\"key\":\"value\"}",
        "[1,2,3]",
        "<script>alert(1)</script>",
        "'; DROP TABLE users; --"
    })
    void create_specialCharacters_preservedRoundTrip(String value) {
        repo.create(obj("data", value));
        String stored = repo.findById(1L).orElseThrow().getFieldAsString("data");
        assertEquals(value, stored);
    }

    @Test
    @DisplayName("유니코드(한글·이모지·CJK) 값도 저장·복원됨")
    void create_unicodeValues_preserved() {
        repo.create(obj("name", "홍길동", "emoji", "😀🚀", "cjk", "漢字"));
        Item item = repo.findById(1L).orElseThrow();
        assertEquals("홍길동", item.getFieldAsString("name"));
        assertEquals("😀🚀",   item.getFieldAsString("emoji"));
        assertEquals("漢字",   item.getFieldAsString("cjk"));
    }

    @Test
    @DisplayName("매우 긴 문자열(100,000자)도 저장·복원됨")
    void create_veryLongString_preserved() {
        String longValue = "A".repeat(100_000);
        repo.create(obj("data", longValue));
        assertEquals(longValue, repo.findById(1L).orElseThrow().getFieldAsString("data"));
    }

    @Test
    @DisplayName("문자열 'null'은 JSON null이 아닌 문자열로 저장됨")
    void create_literalNullString_storedAsString() {
        repo.create(obj("name", "null"));
        String val = repo.findById(1L).orElseThrow().getFieldAsString("name");
        assertEquals("null", val);
    }

    @Test
    @DisplayName("숫자처럼 보이는 문자열도 문자열로 저장됨")
    void create_numericString_storedAsString() {
        repo.create(obj("code", "00123", "big", "99999999999999999999"));
        assertEquals("00123",                repo.findById(1L).orElseThrow().getFieldAsString("code"));
        assertEquals("99999999999999999999", repo.findById(1L).orElseThrow().getFieldAsString("big"));
    }

    @Test
    @DisplayName("SQL Injection 시도 문자열도 안전하게 저장됨")
    void create_sqlInjection_storedSafely() {
        String sql = "' OR '1'='1'; DROP TABLE items; --";
        repo.create(obj("query", sql));
        assertEquals(sql, repo.findById(1L).orElseThrow().getFieldAsString("query"));
    }

    @Test
    @DisplayName("경로 탐색(Path Traversal) 시도 문자열도 안전하게 저장됨")
    void create_pathTraversal_storedSafely() {
        String path = "../../../../etc/passwd";
        repo.create(obj("path", path));
        assertEquals(path, repo.findById(1L).orElseThrow().getFieldAsString("path"));
    }

    @Test
    @DisplayName("필드가 하나도 없는 빈 객체도 생성 가능")
    void create_emptyObject_idAssigned() {
        Item item = repo.create(Json.object());
        assertEquals(1L, item.getId());
    }

    @Test
    @DisplayName("대량(1,000건) 생성 후 전체 조회 개수 일치")
    void create_massInsert_countMatches() {
        for (int i = 0; i < 1_000; i++) {
            repo.create(obj("index", String.valueOf(i)));
        }
        assertEquals(1_000, repo.findAll().size());
    }

    @Test
    @DisplayName("대량 생성 후 마지막 ID는 1,000")
    void create_massInsert_lastIdIs1000() {
        for (int i = 0; i < 1_000; i++) repo.create(obj("i", String.valueOf(i)));
        assertEquals(1_000L, repo.findAll().get(999).getId());
    }

    // ── Read: 잘못된 ID 조회 ─────────────────────────────────────────

    @Test
    @DisplayName("음수 ID 조회 시 Optional.empty() 반환")
    void findById_negativeId_returnsEmpty() {
        repo.create(obj("name", "Alice"));
        assertTrue(repo.findById(-1L).isEmpty());
    }

    @Test
    @DisplayName("0번 ID 조회 시 Optional.empty() 반환")
    void findById_zeroId_returnsEmpty() {
        repo.create(obj("name", "Alice"));
        assertTrue(repo.findById(0L).isEmpty());
    }

    @Test
    @DisplayName("Long 최댓값 ID 조회 시 Optional.empty() 반환")
    void findById_maxLongId_returnsEmpty() {
        repo.create(obj("name", "Alice"));
        assertTrue(repo.findById(Long.MAX_VALUE).isEmpty());
    }

    // ── Search: 이상한 검색어 ─────────────────────────────────────────

    @Test
    @DisplayName("빈 검색어는 해당 필드가 있는 모든 항목 반환")
    void searchByField_emptyKeyword_returnsAllWithField() {
        repo.create(obj("name", "Alice"));
        repo.create(obj("name", "Bob"));
        repo.create(obj("email", "c@c.com"));  // name 필드 없음
        List<Item> result = repo.searchByField("name", "");
        assertEquals(2, result.size());
    }

    @ParameterizedTest(name = "정규식 특수문자 검색: {0}")
    @DisplayName("정규식 특수문자가 검색어에 포함되어도 예외 없이 동작")
    @ValueSource(strings = { ".", "*", "+", "?", "(", ")", "[", "]", "{", "}", "^", "$", "|", "\\" })
    void searchByField_regexSpecialChars_noException(String specialChar) {
        repo.create(obj("name", "Alice"));
        assertDoesNotThrow(() -> repo.searchByField("name", specialChar));
    }

    @Test
    @DisplayName("존재하지 않는 필드로 검색 시 빈 리스트 반환")
    void searchByField_nonExistentField_returnsEmpty() {
        repo.create(obj("name", "Alice"));
        assertTrue(repo.searchByField("phantom", "Alice").isEmpty());
    }

    @Test
    @DisplayName("데이터가 없을 때 검색 시 빈 리스트 반환")
    void searchByField_emptyRepo_returnsEmpty() {
        assertTrue(repo.searchByField("name", "Alice").isEmpty());
    }

    @Test
    @DisplayName("매우 긴 검색어도 예외 없이 동작")
    void searchByField_veryLongKeyword_noException() {
        repo.create(obj("name", "Alice"));
        String longKeyword = "X".repeat(100_000);
        assertDoesNotThrow(() -> repo.searchByField("name", longKeyword));
        assertTrue(repo.searchByField("name", longKeyword).isEmpty());
    }

    // ── Update: 잘못된 요청 ──────────────────────────────────────────

    @Test
    @DisplayName("id 필드는 수정 불가 — false 반환")
    void update_idField_blockedAndReturnsFalse() {
        repo.create(obj("name", "Alice"));
        assertFalse(repo.update(1L, "id", "9999"));
        assertEquals(1L, repo.findById(1L).orElseThrow().getId());  // id 변경 없음
    }

    @Test
    @DisplayName("존재하지 않는 ID 수정 시 false 반환, 데이터 변경 없음")
    void update_nonExistentId_returnsFalseNoSideEffect() {
        repo.create(obj("name", "Alice"));
        assertFalse(repo.update(999L, "name", "Hacker"));
        assertEquals("Alice", repo.findById(1L).orElseThrow().getFieldAsString("name"));
    }

    @Test
    @DisplayName("빈 문자열로 update 시 빈 값으로 덮어씀")
    void update_emptyValue_overwritesWithEmpty() {
        repo.create(obj("name", "Alice"));
        repo.update(1L, "name", "");
        assertEquals("", repo.findById(1L).orElseThrow().getFieldAsString("name"));
    }

    @Test
    @DisplayName("JSON 특수문자가 포함된 값으로 update해도 안전하게 저장됨")
    void update_valueWithJsonSpecialChars_savedSafely() {
        repo.create(obj("name", "Alice"));
        String tricky = "He said \\\"hello\\\" and left\nnewline";
        repo.update(1L, "name", tricky);
        assertEquals(tricky, repo.findById(1L).orElseThrow().getFieldAsString("name"));
    }

    @Test
    @DisplayName("데이터가 없는 repo에서 update 시 false 반환")
    void update_emptyRepo_returnsFalse() {
        assertFalse(repo.update(1L, "name", "Ghost"));
    }

    // ── Delete: 잘못된 요청 ──────────────────────────────────────────

    @Test
    @DisplayName("존재하지 않는 ID 삭제 시 false 반환, 데이터 변경 없음")
    void delete_nonExistentId_returnsFalseNoSideEffect() {
        repo.create(obj("name", "Alice"));
        assertFalse(repo.delete(999L));
        assertEquals(1, repo.findAll().size());
    }

    @Test
    @DisplayName("음수 ID 삭제 시 false 반환")
    void delete_negativeId_returnsFalse() {
        repo.create(obj("name", "Alice"));
        assertFalse(repo.delete(-1L));
    }

    @Test
    @DisplayName("빈 repo에서 삭제 시 false 반환, 예외 없음")
    void delete_emptyRepo_returnsFalseNoException() {
        assertDoesNotThrow(() -> assertFalse(repo.delete(1L)));
    }

    @Test
    @DisplayName("같은 ID를 두 번 삭제해도 두 번째는 false 반환")
    void delete_sameTwice_secondReturnsFalse() {
        repo.create(obj("name", "Alice"));
        assertTrue(repo.delete(1L));
        assertFalse(repo.delete(1L));
    }

    @Test
    @DisplayName("전체 삭제 후 목록은 빈 리스트")
    void delete_all_findAllEmpty() {
        repo.create(obj("name", "Alice"));
        repo.create(obj("name", "Bob"));
        repo.delete(1L);
        repo.delete(2L);
        assertTrue(repo.findAll().isEmpty());
    }

    // ── 파일 손상·비정상 상태 ────────────────────────────────────────

    @Test
    @DisplayName("파일 내용이 깨진 JSON이면 create 시 JsonException 발생")
    void corruptFile_create_throwsJsonException() throws IOException {
        Files.writeString(dataFile, "{ NOT VALID JSON !!!");
        JsonFileRepository brokenRepo = new JsonFileRepository(dataFile.toString());
        assertThrows(JsonException.class, () -> brokenRepo.create(obj("name", "Alice")));
    }

    @Test
    @DisplayName("파일 내용이 깨진 JSON이면 findAll 시 JsonException 발생")
    void corruptFile_findAll_throwsJsonException() throws IOException {
        Files.writeString(dataFile, "GARBAGE");
        JsonFileRepository brokenRepo = new JsonFileRepository(dataFile.toString());
        assertThrows(JsonException.class, brokenRepo::findAll);
    }

    @Test
    @DisplayName("파일 내용이 빈 문자열이면 JsonException 발생")
    void emptyFile_findAll_throwsJsonException() throws IOException {
        Files.writeString(dataFile, "");
        JsonFileRepository brokenRepo = new JsonFileRepository(dataFile.toString());
        assertThrows(JsonException.class, brokenRepo::findAll);
    }

    @Test
    @DisplayName("파일 내용이 JSON 배열이면 래퍼 포맷 불일치로 JsonException 발생")
    void fileContainsArray_findAll_throwsJsonException() throws IOException {
        // 구 포맷(배열)은 현재 포맷(래퍼 객체)과 호환되지 않음
        Files.writeString(dataFile, "[{\"id\":1,\"name\":\"Alice\"}]");
        JsonFileRepository brokenRepo = new JsonFileRepository(dataFile.toString());
        assertThrows(JsonException.class, brokenRepo::findAll);
    }

    @Test
    @DisplayName("파일 내용이 문자열이면 JsonException 발생")
    void fileContainsString_findAll_throwsJsonException() throws IOException {
        Files.writeString(dataFile, "\"just a string\"");
        JsonFileRepository brokenRepo = new JsonFileRepository(dataFile.toString());
        assertThrows(JsonException.class, brokenRepo::findAll);
    }

    @Test
    @DisplayName("올바른 래퍼 포맷이지만 items 키가 없으면 JsonException 발생")
    void fileWrapperMissingItemsKey_throwsJsonException() throws IOException {
        Files.writeString(dataFile, "{\"seq\":0}");
        JsonFileRepository brokenRepo = new JsonFileRepository(dataFile.toString());
        assertThrows(JsonException.class, brokenRepo::findAll);
    }

    // ── 복합 시나리오 ─────────────────────────────────────────────────

    @Test
    @DisplayName("생성·삭제·생성을 반복해도 ID는 단조 증가")
    void mixed_createDeleteCreate_idMonotonicallyIncreasing() {
        repo.create(obj("n", "A")); // id=1
        repo.create(obj("n", "B")); // id=2
        repo.delete(1L);
        repo.delete(2L);
        repo.create(obj("n", "C")); // id=3 (1,2 재사용 안 함)
        assertEquals(3L, repo.findAll().get(0).getId());
    }

    @Test
    @DisplayName("동일한 값을 가진 항목을 여러 개 생성해도 각각 고유한 ID")
    void create_duplicateValues_uniqueIds() {
        repo.create(obj("name", "Alice"));
        repo.create(obj("name", "Alice"));
        repo.create(obj("name", "Alice"));
        List<Item> all = repo.findAll();
        assertEquals(3, all.size());
        assertNotEquals(all.get(0).getId(), all.get(1).getId());
        assertNotEquals(all.get(1).getId(), all.get(2).getId());
    }

    @Test
    @DisplayName("update 후 searchByField가 변경된 값으로 검색 가능")
    void update_thenSearch_findsByNewValue() {
        repo.create(obj("name", "Alice"));
        repo.update(1L, "name", "Alicia");
        assertEquals(1, repo.searchByField("name", "Alicia").size());
        assertTrue(repo.searchByField("name", "Alice").isEmpty());  // 이전 값 사라짐
    }

    @Test
    @DisplayName("필드명에 공백이 포함된 경우도 저장·조회 가능")
    void create_fieldNameWithSpaces_storedAndRetrievable() {
        JsonObject data = Json.object();
        data.put("full name", "Alice Smith");
        repo.create(data);
        assertEquals("Alice Smith",
                repo.findById(1L).orElseThrow().getFieldAsString("full name"));
    }

    @Test
    @DisplayName("키에 JSON 특수문자가 있어도 저장·조회 가능")
    void create_fieldNameWithSpecialChars_storedAndRetrievable() {
        JsonObject data = Json.object();
        data.put("key\"with\"quotes", "value");
        repo.create(data);
        assertNotNull(repo.findById(1L).orElseThrow()
                .getFieldAsString("key\"with\"quotes"));
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────

    private static JsonObject obj(String... kv) {
        JsonObject o = Json.object();
        for (int i = 0; i < kv.length; i += 2) o.put(kv[i], kv[i + 1]);
        return o;
    }
}
