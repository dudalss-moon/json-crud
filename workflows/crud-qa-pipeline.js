/**
 * JSON CRUD QA 파이프라인
 *
 * 실행 방법 (Claude Code):
 *   Workflow({ scriptPath: "workflows/crud-qa-pipeline.js" })
 *
 * 파이프라인 구조:
 *   SubAgent1 (문서 정합성 검증)
 *       ↓
 *   SubAgent2 (AI Action — 이슈 수정)
 *       ↓
 *   SubAgent3 ──┐  병렬
 *   SubAgent4 ──┘
 *       ↓
 *   최종 리포트 (ALL GREEN / ISSUES FOUND)
 */

export const meta = {
  name: 'crud-qa-pipeline',
  description: 'JSON CRUD QA: 문서 정합성 → 코드 생성 → (테스트 + 컴플라이언스) 병렬 검증',
  phases: [
    { title: 'SubAgent1: 문서 정합성 검증' },
    { title: 'SubAgent2: AI Action' },
    { title: 'SubAgent3: Test Verify' },
    { title: 'SubAgent4: Compliance Verify' },
  ],
}

const DOC_REPORT_SCHEMA = {
  type: 'object',
  properties: {
    issues: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          severity:    { type: 'string', enum: ['critical', 'warning', 'info'] },
          location:    { type: 'string' },
          description: { type: 'string' },
          suggestion:  { type: 'string' },
        },
        required: ['severity', 'location', 'description', 'suggestion'],
      },
    },
    summary:   { type: 'string' },
    hasIssues: { type: 'boolean' },
  },
  required: ['issues', 'summary', 'hasIssues'],
}

const CODE_ACTION_SCHEMA = {
  type: 'object',
  properties: {
    actionsPerformed: { type: 'array', items: { type: 'string' } },
    filesModified:    { type: 'array', items: { type: 'string' } },
    summary:          { type: 'string' },
    noActionNeeded:   { type: 'boolean' },
  },
  required: ['actionsPerformed', 'filesModified', 'summary', 'noActionNeeded'],
}

const TEST_RESULT_SCHEMA = {
  type: 'object',
  properties: {
    totalTests:  { type: 'number' },
    passed:      { type: 'number' },
    failed:      { type: 'number' },
    failures:    { type: 'array', items: { type: 'string' } },
    e2eIsolated: { type: 'boolean' },
    summary:     { type: 'string' },
    allPassed:   { type: 'boolean' },
  },
  required: ['totalTests', 'passed', 'failed', 'failures', 'e2eIsolated', 'summary', 'allPassed'],
}

const COMPLIANCE_SCHEMA = {
  type: 'object',
  properties: {
    checks: {
      type: 'array',
      items: {
        type: 'object',
        properties: {
          requirement: { type: 'string' },
          status:      { type: 'string', enum: ['pass', 'fail', 'partial'] },
          evidence:    { type: 'string' },
        },
        required: ['requirement', 'status', 'evidence'],
      },
    },
    compliant: { type: 'boolean' },
    summary:   { type: 'string' },
  },
  required: ['checks', 'compliant', 'summary'],
}

// ── SubAgent1: 문서 정합성 검증 ──────────────────────────────────────
phase('SubAgent1: 문서 정합성 검증')

const docReport = await agent(
  `프로젝트 경로: C:\\reviewer\\json-crud

다음 파일들을 모두 읽고 문서 간 정합성을 검증하라.

읽어야 할 파일:
- README.md
- build.gradle
- src/main/java/com/crud/Main.java
- src/main/java/com/crud/repository/JsonFileRepository.java
- src/main/java/com/crud/ui/ConsoleApp.java
- src/test/java/com/crud/repository/JsonFileRepositoryRegressionTest.java
- src/test/java/com/crud/repository/JsonFileRepositorySafetyTest.java

검증 항목:
1. README의 데이터 저장 포맷({"seq":N,"items":[...]})이 JsonFileRepository 코드와 일치하는가?
2. README의 CRUD 기능 6가지가 ConsoleApp·JsonFileRepository에 모두 구현되어 있는가?
3. README의 실행 방법(mainClass, 기본 경로 data/items.json)이 build.gradle·Main.java와 일치하는가?
4. README의 요구 사항(Java 17+, Gradle)이 build.gradle과 일치하는가?
5. 테스트의 @DisplayName이 실제 검증 로직과 일치하는가?
6. 문서 간 중복·모순·잘못된 코드 예시가 있는가?
7. README의 테스트 섹션에 명시된 테스트 개수(26개, 61개)가 실제 테스트 클래스와 일치하는가?

각 이슈마다 severity(critical/warning/info), location, description, suggestion을 기재하라.
문제가 없으면 hasIssues: false로 반환하라.`,
  { label: 'SubAgent1', schema: DOC_REPORT_SCHEMA }
)

log('SubAgent1 완료 — ' + (docReport.hasIssues ? docReport.issues.length + '개 이슈' : '이슈 없음'))

// ── SubAgent2: AI Action (Plan → 코드 생성/수정) ───────────────────
phase('SubAgent2: AI Action')

const issuesSummary = docReport.hasIssues
  ? docReport.issues.map(function(i) {
      return '[' + i.severity + '] ' + i.location + ': ' + i.description + ' → ' + i.suggestion
    }).join('\n')
  : '없음'

const codeAction = await agent(
  `프로젝트 경로: C:\\reviewer\\json-crud

SubAgent1 문서 정합성 검증 결과:
${issuesSummary}

위 결과를 바탕으로 Plan(README.md 명세)에 맞게 코드와 문서를 수정하라.

처리 원칙:
- critical 이슈: 반드시 수정
- warning 이슈: 수정 권장
- info 이슈: 판단 후 결정
- 파일 수정 전 반드시 Read로 현재 내용 확인 후 Edit으로 최소 변경
- 기능 동작을 바꾸지 않고 불일치만 해소
- 코드가 맞고 문서가 틀렸으면 문서를 수정
- 이슈가 없으면 noActionNeeded: true 반환, 파일 수정 금지

수행한 액션을 actionsPerformed에, 수정한 파일을 filesModified에 기록하라.`,
  { label: 'SubAgent2', schema: CODE_ACTION_SCHEMA }
)

log('SubAgent2 완료 — ' + (codeAction.noActionNeeded ? '수정 없음' : codeAction.filesModified.join(', ')))

// ── SubAgent3 + SubAgent4: 병렬 실행 ──────────────────────────────
const parallelResults = await parallel([

  function() {
    return agent(
      `프로젝트 경로: C:\\reviewer\\json-crud
JAVA_HOME: C:\\Users\\User\\.jdks\\temurin-17.0.19

[Unit Test 실행]
PowerShell로 다음 명령을 실행하라:
  $env:JAVA_HOME = "C:\\Users\\User\\.jdks\\temurin-17.0.19"; cd "C:\\reviewer\\json-crud"; .\\gradlew.bat test --rerun-tasks 2>&1

[결과 수집]
테스트 완료 후 아래 XML 파일을 읽어 결과를 파악하라:
  build/test-results/test/TEST-com.crud.repository.JsonFileRepositoryRegressionTest.xml
  build/test-results/test/TEST-com.crud.repository.JsonFileRepositorySafetyTest.xml

XML testsuite 태그의 tests/failures/errors 속성으로 통계를 계산하라.
실패한 testcase의 name을 failures 배열에 포함하라.

[E2E 격리 확인]
테스트 클래스에서 @TempDir 사용 여부를 확인하라.
테스트가 data/items.json 실제 파일을 오염시키지 않는지(e2eIsolated) 판단하라.`,
      { label: 'SubAgent3: Test Verify', phase: 'SubAgent3: Test Verify', schema: TEST_RESULT_SCHEMA }
    )
  },

  function() {
    return agent(
      `프로젝트 경로: C:\\reviewer\\json-crud

README.md의 명세(Plan)와 실제 구현 코드의 컴플라이언스를 검증하라.

읽어야 할 파일:
- README.md
- src/main/java/com/crud/repository/JsonFileRepository.java
- src/main/java/com/crud/ui/ConsoleApp.java
- src/main/java/com/crud/Main.java
- build.gradle

체크리스트 (각 항목: requirement, status(pass/fail/partial), evidence(파일:라인)):
1. Create — 필드 자유 입력, id 자동 생성, JSON 파일 저장
2. Read All — 전체 항목 출력
3. Read by ID — 숫자 ID 단건 조회, 미존재 시 안내
4. Search by Field — 부분 일치 검색, 대소문자 무시
5. Update — 필드 수정, id 필드 수정 차단
6. Delete — 삭제 전 미리보기, y/N 확인 절차
7. 데이터 포맷 — {"seq":N,"items":[...]} 래퍼 구조
8. ID 재사용 없음 — 삭제 후 seq 감소 안 함
9. 진입점 — com.crud.Main, 기본 경로 data/items.json
10. 빌드 설정 — application 플러그인, mainClass, UTF-8 인코딩
11. id 필드 보호 — ConsoleApp과 Repository 양쪽에서 수정 차단
12. 삭제 확인 — y 입력 시에만 삭제, 그 외 취소

모든 항목이 pass이면 compliant: true`,
      { label: 'SubAgent4: Compliance Verify', phase: 'SubAgent4: Compliance Verify', schema: COMPLIANCE_SCHEMA }
    )
  },

])

const testResult       = parallelResults[0]
const complianceResult = parallelResults[1]

log('SubAgent3 완료 — ' + (testResult ? testResult.summary : 'null'))
log('SubAgent4 완료 — ' + (complianceResult ? complianceResult.summary : 'null'))

// ── 최종 리포트 ────────────────────────────────────────────────────
const allGreen =
  !docReport.hasIssues &&
  codeAction.noActionNeeded &&
  testResult && testResult.allPassed &&
  complianceResult && complianceResult.compliant

return {
  pipeline: 'crud-qa-pipeline',
  overallResult: allGreen ? 'ALL GREEN' : 'ISSUES FOUND',
  subAgent1_문서정합성:   docReport,
  subAgent2_코드액션:     codeAction,
  subAgent3_테스트:       testResult,
  subAgent4_컴플라이언스: complianceResult,
}
