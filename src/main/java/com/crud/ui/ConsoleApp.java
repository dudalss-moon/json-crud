package com.crud.ui;

import com.crud.model.Item;
import com.crud.repository.JsonFileRepository;
import com.jsonlib.Json;
import com.jsonlib.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * JSON CRUD 콘솔 애플리케이션.
 * 사용자로부터 입력을 받아 JsonFileRepository를 통해 데이터를 관리한다.
 */
public class ConsoleApp {

    private static final String SEPARATOR = "─".repeat(50);

    private final JsonFileRepository repo;
    private final Scanner scanner;

    public ConsoleApp(String dataFilePath) {
        this.repo = new JsonFileRepository(dataFilePath);
        this.scanner = new Scanner(System.in);
    }

    public void run() {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║        JSON CRUD 콘솔 애플리케이션               ║");
        System.out.println("╚══════════════════════════════════════════════════╝");

        while (true) {
            printMainMenu();
            String choice = prompt("메뉴 선택").trim();

            switch (choice) {
                case "1" -> handleCreate();
                case "2" -> handleReadAll();
                case "3" -> handleSearchById();
                case "4" -> handleSearchByField();
                case "5" -> handleUpdate();
                case "6" -> handleDelete();
                case "0" -> {
                    System.out.println("\n프로그램을 종료합니다.");
                    return;
                }
                default -> System.out.println("  [오류] 잘못된 선택입니다.");
            }
        }
    }

    // ── 메뉴 출력 ────────────────────────────────────────────────────

    private void printMainMenu() {
        System.out.println("\n" + SEPARATOR);
        System.out.println("  1. 데이터 추가    (Create)");
        System.out.println("  2. 전체 목록 보기 (Read All)");
        System.out.println("  3. ID로 검색      (Read by ID)");
        System.out.println("  4. 필드로 검색    (Search by Field)");
        System.out.println("  5. 데이터 수정    (Update)");
        System.out.println("  6. 데이터 삭제    (Delete)");
        System.out.println("  0. 종료");
        System.out.println(SEPARATOR);
    }

    // ── Create ───────────────────────────────────────────────────────

    private void handleCreate() {
        System.out.println("\n[데이터 추가]");
        System.out.println("저장할 필드를 입력하세요. 입력 완료 후 빈 줄을 입력하면 저장됩니다.");

        JsonObject data = Json.object();
        while (true) {
            String fieldName = prompt("필드명 (완료: 엔터)").trim();
            if (fieldName.isEmpty()) break;
            if ("id".equals(fieldName)) {
                System.out.println("  [안내] 'id' 필드는 자동 생성됩니다. 다른 필드명을 입력하세요.");
                continue;
            }
            String fieldValue = prompt("  " + fieldName + " 값").trim();
            data.put(fieldName, fieldValue);
        }

        if (data.isEmpty()) {
            System.out.println("  [취소] 입력된 필드가 없어 저장하지 않았습니다.");
            return;
        }

        Item created = repo.create(data);
        System.out.println("\n  [저장 완료] ID: " + created.getId());
        printItem(created);
    }

    // ── Read ─────────────────────────────────────────────────────────

    private void handleReadAll() {
        System.out.println("\n[전체 목록]");
        List<Item> items = repo.findAll();
        if (items.isEmpty()) {
            System.out.println("  저장된 데이터가 없습니다.");
            return;
        }
        System.out.println("  총 " + items.size() + "건");
        items.forEach(this::printItem);
    }

    private void handleSearchById() {
        System.out.println("\n[ID로 검색]");
        String input = prompt("검색할 ID").trim();
        long id;
        try {
            id = Long.parseLong(input);
        } catch (NumberFormatException e) {
            System.out.println("  [오류] 숫자로 입력해주세요.");
            return;
        }

        Optional<Item> found = repo.findById(id);
        if (found.isPresent()) {
            System.out.println("  [결과]");
            printItem(found.get());
        } else {
            System.out.println("  ID " + id + "에 해당하는 데이터가 없습니다.");
        }
    }

    private void handleSearchByField() {
        System.out.println("\n[필드로 검색]");
        String key     = prompt("검색할 필드명").trim();
        String keyword = prompt("검색어").trim();

        List<Item> results = repo.searchByField(key, keyword);
        if (results.isEmpty()) {
            System.out.println("  검색 결과가 없습니다.");
        } else {
            System.out.println("  총 " + results.size() + "건 검색됨");
            results.forEach(this::printItem);
        }
    }

    // ── Update ───────────────────────────────────────────────────────

    private void handleUpdate() {
        System.out.println("\n[데이터 수정]");
        String input = prompt("수정할 데이터의 ID").trim();
        long id;
        try {
            id = Long.parseLong(input);
        } catch (NumberFormatException e) {
            System.out.println("  [오류] 숫자로 입력해주세요.");
            return;
        }

        Optional<Item> found = repo.findById(id);
        if (found.isEmpty()) {
            System.out.println("  ID " + id + "에 해당하는 데이터가 없습니다.");
            return;
        }

        System.out.println("  [현재 데이터]");
        printItem(found.get());

        String key      = prompt("수정할 필드명").trim();
        String newValue = prompt("새 값").trim();

        if ("id".equals(key)) {
            System.out.println("  [오류] id 필드는 수정할 수 없습니다.");
            return;
        }

        boolean updated = repo.update(id, key, newValue);
        if (updated) {
            System.out.println("  [수정 완료]");
            repo.findById(id).ifPresent(this::printItem);
        } else {
            System.out.println("  [오류] 수정에 실패했습니다.");
        }
    }

    // ── Delete ───────────────────────────────────────────────────────

    private void handleDelete() {
        System.out.println("\n[데이터 삭제]");
        String input = prompt("삭제할 데이터의 ID").trim();
        long id;
        try {
            id = Long.parseLong(input);
        } catch (NumberFormatException e) {
            System.out.println("  [오류] 숫자로 입력해주세요.");
            return;
        }

        Optional<Item> found = repo.findById(id);
        if (found.isEmpty()) {
            System.out.println("  ID " + id + "에 해당하는 데이터가 없습니다.");
            return;
        }

        System.out.println("  [삭제 대상]");
        printItem(found.get());

        String confirm = prompt("정말 삭제하시겠습니까? (y/N)").trim();
        if ("y".equalsIgnoreCase(confirm)) {
            repo.delete(id);
            System.out.println("  [삭제 완료] ID " + id + " 항목이 삭제되었습니다.");
        } else {
            System.out.println("  [취소] 삭제를 취소했습니다.");
        }
    }

    // ── 출력 헬퍼 ────────────────────────────────────────────────────

    private void printItem(Item item) {
        System.out.println("  " + SEPARATOR.substring(0, 30));
        String[] lines = item.toString().split("\n");
        for (String line : lines) {
            System.out.println("  " + line);
        }
    }

    private String prompt(String message) {
        System.out.print("  > " + message + ": ");
        return scanner.hasNextLine() ? scanner.nextLine() : "";
    }
}
