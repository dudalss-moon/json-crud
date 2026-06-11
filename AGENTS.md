# AGENTS.md

이 프로젝트에서 동작하는 AI 에이전트들의 역할, 권한, 협업 규칙을 정의합니다.
에이전트 정의 파일은 `.claude/agents/` 에 위치합니다.

---

## 에이전트 목록

| 에이전트 | 파일 | 권한 | 역할 |
|----------|------|------|------|
| `doc-consistency` | `.claude/agents/doc-consistency.md` | 읽기 전용 | 문서 간 충돌·중복·모순 탐지 |
| `ai-action` | `.claude/agents/ai-action.md` | 읽기 + 수정 | 이슈 기반 코드·문서 수정 |
| `test-verify` | `.claude/agents/test-verify.md` | 읽기 + 실행 | 테스트 실행 및 결과 검증 |
| `compliance-verify` | `.claude/agents/compliance-verify.md` | 읽기 전용 | Plan vs 구현 컴플라이언스 판정 |

---

## 역할 상세

### SubAgent1 — `doc-consistency` (문서 정합성 검증)

**목적**: 코드를 수정하기 전, 문서와 코드 사이의 불일치를 먼저 파악한다.

**검증 대상**:
- `README.md` ↔ `JsonFileRepository.java` (데이터 포맷 일치 여부)
- `README.md` ↔ `ConsoleApp.java` (CRUD 기능 6가지 구현 여부)
- `README.md` ↔ `build.gradle` (실행 방법, Java 버전 요구사항)
- `@DisplayName` ↔ 실제 테스트 로직 (테스트 설명 정확성)

**허용 도구**: `Read`, `Glob`, `Grep`
**금지**: 파일 수정 불가 (`Edit`, `Write`, `Bash` 사용 금지)

---

### SubAgent2 — `ai-action` (AI Action)

**목적**: `doc-consistency`가 발견한 이슈를 받아 Plan(README.md)에 맞게 파일을 수정한다.

**처리 우선순위**:
- `critical` → 반드시 수정
- `warning`  → 수정 권장
- `info`     → 판단 후 결정

**수정 원칙**:
- 파일 수정 전 반드시 `Read` 먼저 수행
- 기능 동작을 바꾸지 않고 불일치만 해소
- 이슈가 없으면 파일을 수정하지 않음

**허용 도구**: `Read`, `Edit`, `Write`, `Grep`, `Glob`

---

### SubAgent3 — `test-verify` (Test Verify)

**목적**: 전체 테스트 스위트를 실행하고 Unit Test 통과 여부와 E2E 격리 상태를 보고한다.

**수행 작업**:
1. `./gradlew test --rerun-tasks` 실행
2. `build/test-results/test/*.xml` 파싱 → 통과/실패 통계 산출
3. `@TempDir` 사용 여부 확인 → `data/items.json` 오염 방지 검증

**허용 도구**: `Bash`, `Read`, `Glob`

---

### SubAgent4 — `compliance-verify` (Compliance Verify)

**목적**: README.md의 명세(Plan) 12개 항목이 실제 코드에 모두 구현되었는지 판정한다.

**체크리스트**:

| # | 항목 |
|---|------|
| 1 | Create — 필드 자유 입력, id 자동 생성 |
| 2 | Read All — 전체 항목 출력 |
| 3 | Read by ID — 숫자 ID 단건 조회 |
| 4 | Search by Field — 부분 일치, 대소문자 무시 |
| 5 | Update — 필드 수정, id 수정 차단 |
| 6 | Delete — 미리보기, y/N 확인 |
| 7 | 데이터 포맷 `{"seq":N,"items":[...]}` |
| 8 | ID 재사용 없음 (seq 단조 증가) |
| 9 | 진입점 `com.crud.Main`, 기본 경로 `data/items.json` |
| 10 | 빌드 설정 — application 플러그인, mainClass, UTF-8 |
| 11 | id 필드 보호 — ConsoleApp·Repository 양쪽 차단 |
| 12 | 삭제 확인 — y 입력 시에만 삭제 |

**판정 기준**: 모든 항목 `pass` → 컴플라이언스 적합 / 하나라도 `fail` → 부적합

**허용 도구**: `Read`, `Grep`, `Glob`
**금지**: 파일 수정 불가

---

## 파이프라인 실행 순서

```
SubAgent1 (doc-consistency)
    │  이슈 목록 전달
    ▼
SubAgent2 (ai-action)
    │  수정 완료 신호
    ▼
SubAgent3 (test-verify) ──┐
                          │ 병렬 실행
SubAgent4 (compliance-verify) ──┘
    │
    ▼
최종 리포트 (ALL GREEN / ISSUES FOUND)
```

파이프라인 스크립트: `workflows/crud-qa-pipeline.js`

---

## 공통 규칙

1. **파일 수정 전 항상 Read 먼저** — 현재 상태 확인 없이 Edit 금지
2. **읽기 전용 에이전트는 Edit·Write·Bash 사용 금지** (`doc-consistency`, `compliance-verify`)
3. **최소 변경 원칙** — 요청된 범위 밖의 코드 수정 금지
4. **출력은 구조화** — 판단 근거(파일명:라인)를 항상 포함
5. **이슈 없으면 보고만** — 수정할 이슈가 없을 때 파일에 손대지 않음
