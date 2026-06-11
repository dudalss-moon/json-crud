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

모든 데이터는 `data/items.json`에 JSON 배열로 저장됩니다.

```json
[
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
```

---

## 관련 프로젝트

- [json-poc](https://github.com/dudalss-moon/json-poc) — 본 프로젝트에서 사용한 커스텀 JSON 라이브러리 PoC
