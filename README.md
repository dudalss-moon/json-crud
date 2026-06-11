# JSON CRUD 콘솔 애플리케이션

PoC 프로젝트([json-poc](https://github.com/dudalss-moon/json-poc))의 커스텀 JSON 라이브러리 구조를 유지하면서,  
데이터를 JSON 파일로 관리하는 CRUD 콘솔 애플리케이션입니다.

---

## 프로젝트 구조

```
src/main/java/
├── com/jsonlib/                          ← PoC 원본 JSON 라이브러리
│   ├── Json.java                         파싱·저장·읽기 퍼사드
│   ├── JsonValue.java                    모든 JSON 타입의 추상 기반 클래스
│   ├── JsonObject.java                   JSON 오브젝트 { }
│   ├── JsonArray.java                    JSON 배열 [ ]
│   ├── JsonParser.java                   JSON 문자열 파서
│   ├── JsonWriter.java                   JSON 직렬화 및 파일 I/O
│   ├── JsonString / JsonNumber /
│   │   JsonBoolean / JsonNull.java       원시 타입 래퍼
│   └── JsonException.java               커스텀 예외
│
└── com/crud/                             ← CRUD 애플리케이션
    ├── Main.java                         진입점
    ├── model/Item.java                   JsonObject 기반 데이터 모델
    ├── repository/
    │   └── JsonFileRepository.java       JSON 파일 CRUD 데이터 레이어
    └── ui/ConsoleApp.java                콘솔 메뉴 UI

data/
└── items.json                            ← 런타임 생성, 데이터 저장 파일
```

---

## 요구 사항

| 항목 | 버전 |
|------|------|
| Java | 17 이상 |
| Gradle | 래퍼 포함 (별도 설치 불필요) |

---

## 실행 방법

```bash
# Windows
.\gradlew.bat run

# macOS / Linux
./gradlew run
```

데이터 파일 경로를 직접 지정하려면:

```bash
.\gradlew.bat run --args="path/to/mydata.json"
```

> 처음 실행 시 `data/items.json` 파일이 자동으로 생성됩니다.

---

## CRUD 기능

실행하면 아래 메뉴가 표시됩니다.

```
──────────────────────────────────────────────────
  1. 데이터 추가    (Create)
  2. 전체 목록 보기 (Read All)
  3. ID로 검색      (Read by ID)
  4. 필드로 검색    (Search by Field)
  5. 데이터 수정    (Update)
  6. 데이터 삭제    (Delete)
  0. 종료
──────────────────────────────────────────────────
```

### 1. Create — 데이터 추가

필드명과 값을 자유롭게 입력합니다. 빈 줄을 입력하면 저장되고 `id`는 자동 생성됩니다.

```
> 필드명 (완료: 엔터): name
>   name 값: Alice
> 필드명 (완료: 엔터): age
>   age 값: 30
> 필드명 (완료: 엔터):         ← 빈 줄 입력 시 저장

[저장 완료] ID: 1
{
  "name": "Alice",
  "age": "30",
  "id": 1
}
```

### 2. Read All — 전체 목록 보기

저장된 모든 항목을 출력합니다.

### 3. Read by ID — ID로 검색

숫자 ID를 입력하면 해당 항목을 조회합니다.

### 4. Search by Field — 필드로 검색

필드명과 검색어를 입력하면 해당 필드의 값에 검색어가 포함된 항목을 모두 반환합니다 (대소문자 무시).

```
> 검색할 필드명: name
> 검색어: ali

총 1건 검색됨
{
  "name": "Alice",
  "age": "30",
  "id": 1
}
```

### 5. Update — 데이터 수정

ID를 선택한 후 수정할 필드명과 새 값을 입력합니다. `id` 필드는 수정할 수 없습니다.

```
> 수정할 데이터의 ID: 1
> 수정할 필드명: age
> 새 값: 31

[수정 완료]
{
  "name": "Alice",
  "age": "31",
  "id": 1
}
```

### 6. Delete — 데이터 삭제

ID를 선택하면 삭제 대상을 미리 보여주고 확인(`y`) 후 삭제합니다.

```
> 삭제할 데이터의 ID: 1

[삭제 대상]
{
  "name": "Alice",
  "age": "31",
  "id": 1
}
> 정말 삭제하시겠습니까? (y/N): y

[삭제 완료] ID 1 항목이 삭제되었습니다.
```

---

## 데이터 저장 형식

모든 데이터는 `data/items.json`에 아래 래퍼 구조로 저장됩니다.

```json
{
  "seq": 2,
  "items": [
    {
      "name": "Alice",
      "age": "31",
      "id": 1
    },
    {
      "name": "Bob",
      "age": "25",
      "id": 2
    }
  ]
}
```

- `seq` : 지금까지 발급한 최대 ID. 항목을 삭제해도 감소하지 않아 **ID가 재사용되지 않습니다**.
- `items` : 현재 저장된 데이터 배열.

---

## 테스트

### 테스트 구조

```
src/test/java/com/crud/repository/
├── JsonFileRepositoryRegressionTest.java   회귀(Regression) 테스트 — 26개
└── JsonFileRepositorySafetyTest.java       안정성(Safety) 테스트  — 61개
```

### 실행 방법

```bash
# Windows
.\gradlew.bat test

# macOS / Linux
./gradlew test

# 결과 리포트 (브라우저로 열기)
build/reports/tests/test/index.html
```

### Regression 테스트 (26개)

정상적인 CRUD 흐름이 올바르게 동작하는지 검증합니다.

| 영역 | 주요 검증 내용 |
|------|----------------|
| Create | ID 자동 증가, 필드 저장·복원, 파일 영속성 |
| Read All | 빈 목록, 전체 조회, 삽입 순서 유지 |
| Find by ID | 정상 조회, 미존재·삭제 후 `Optional.empty()` |
| Search | 완전/부분 일치, 대소문자 무시, 미일치·필드 없음 |
| Update | 값 변경, 다른 필드 무영향, 파일 영속성, 새 필드 추가 |
| Delete | 성공·실패 반환값, 파일 영속성, ID 단조 증가 보장 |

### Safety 테스트 (61개)

악의적이거나 비정상적인 입력에도 안전하게 동작하는지 검증합니다.

| 카테고리 | 주요 검증 내용 |
|----------|----------------|
| 이상한 값 입력 | 빈 문자열·공백, JSON 특수문자 8종, 한글·이모지·CJK 유니코드, 100,000자 초과 문자열, 문자열 `"null"`, 숫자형 문자열 |
| 악의적 입력 | SQL Injection (`' OR '1'='1'`), Path Traversal (`../../../../etc/passwd`) |
| 극단값 | 빈 객체 생성, 1,000건 대량 삽입·조회 |
| 잘못된 ID | 음수, 0, `Long.MAX_VALUE` |
| 이상한 검색어 | 빈 검색어, 정규식 특수문자 14종, 없는 필드, 100,000자 검색어 |
| Update 방어 | `id` 필드 수정 차단, 없는 ID, 빈 값, 특수문자 값 |
| Delete 방어 | 없는 ID, 음수 ID, 빈 저장소, 동일 ID 이중 삭제 |
| 파일 손상 | 깨진 JSON, 빈 파일, 잘못된 포맷, `items` 키 누락 |
| 복합 시나리오 | 생성·삭제 반복 후 ID 단조성, 중복값 고유 ID, update 후 검색 |

### 테스트 중 발견·수정한 버그

> **ID 재사용 문제**: 모든 항목을 삭제하면 다음 ID가 1부터 재시작되는 버그.  
> Safety 테스트에서 발견 후 파일 포맷에 `seq` 카운터를 추가해 수정.

---

## 관련 프로젝트

- [json-poc](https://github.com/dudalss-moon/json-poc) — 본 프로젝트에서 사용한 커스텀 JSON 라이브러리 PoC
